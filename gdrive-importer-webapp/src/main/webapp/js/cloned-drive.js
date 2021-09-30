(function($, utils, cloudDrives) {

    function ClonedDrive() {
        var NOTICE_WIDTH = "380px";
        var prefixUrl = utils.pageBaseUrl(location);
        var contextNode;
        this.init = function(nodeWorkspace, nodePath) {
            try {
                if (nodeWorkspace && nodePath) {
                    contextNode = {
                        workspace: nodeWorkspace,
                        path: nodePath,
                    };
                } else {
                    contextNode = null;
                }
            } catch (e) {
                utils.log("Error initializing Cloud Drive " + e, e);
            }
        };
        var self = this;
        var auth;
        (async() => {
            while(cloudDrives.getProviders().gdrive == undefined)
                await new Promise(resolve => setTimeout(resolve, 1000));
            var providers = cloudDrives.getProviders();
            var gdriveProvider = providers["cgdrive"];
            self.auth = function (folderOrFileId, groupId) {
                var authWindow;
                if (gdriveProvider && gdriveProvider.authURL) {
                    if (gdriveProvider.authURL) {
                        authWindow = window.open(gdriveProvider.authURL, "windowname1", 'width=800, height=600');
                    } else {
                        authService = serviceGet;
                    }
                }
                var process = $.Deferred();
                connectInit(gdriveProvider.id, {
                    done: function(provider) {
                        utils.log(provider.name + " connect initialized.");
                        // 2 wait for authentication
                        var auth = waitAuth(authWindow);
                        auth.done(function() {
                            utils.log(provider.name + " user authenticated.");
                            process.notify({
                                progress: 0,
                                drive: {
                                    provider: provider,
                                },
                            });
                            var targetNode = contextNode;
                            if (targetNode) {
                                utils.log("Connecting Cloud Drive to node " + targetNode.path + " in " + targetNode.workspace);
                                var clone = clonePost(targetNode.workspace, targetNode.path, folderOrFileId, groupId);
                                self.cloneprocess(clone);
                                clone.done(function (state, status) {
                                    utils.log("Connect requested: " + status + ". ");
                                    if (state) {
                                        if (status == 201) {
                                            utils.log("DONE: " + provider.name + " successfully connected.");
                                            self.contextDrive = state.drive;
                                            process.resolve(state);
                                        } else if (status == 202) {
                                           var check = connectCheck(state.serviceUrl);
                                            check.fail(function (error) {
                                                process.reject(error);
                                            });
                                            check.progress(function (state) {
                                                process.notify(state);
                                            });
                                            check.done(function (state) {
                                                self.contextDrive = state.drive;
                                                process.resolve(state);
                                            });
                                        } else {
                                            utils.log("WARN: unexpected state returned from connect service " + status);
                                        }
                                    } else {
                                        utils.log("ERROR: " + provider.name + " connect return null state.");
                                        process.reject("Cannot connect " + provider.name + ". Server return empty response.");
                                    }
                                });
                                clone.fail(function (state, error, errorText) {
                                    utils.log("ERROR: " + provider.name + " connect failed: " + error + ". ");
                                    if (typeof state === "string") {
                                        process.reject(state);
                                    } else {
                                        process.reject(state && state.error ? state.error : error + " " + errorText);
                                    }
                                });
                            } else {
                                process.reject("Connect to " + provider.name + " canceled.");
                            }
                        });
                        auth.fail(function(message) {
                            if (message) {
                                utils.log("ERROR: " + provider.name + " authentication error: " + message);
                            }
                            process.reject(message);
                        });
                    },
                    fail: function(error) {
                        utils.log("ERROR: Connect to Cloud Drive cannot be initiated. " + error);
                        if (authWindow && !authWindow.closed) {
                            authWindow.close();
                        }
                        process.reject(error);
                    },
                });
                return process.promise();
            }
        })();

        var clonePost = function(workspace, path, folderOrFileId, groupId) {
            var request = $.ajax({
                type: "POST",
                url: prefixUrl + "/portal/rest/copygdrive/clone",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                    folderOrFileId: folderOrFileId,
                    groupId: groupId,
                },
                xhrFields: {
                    withCredentials: true,
                },
            });

            return initRequest(request);
        };
        var waitAuth = function(authWindow) {
            var process = $.Deferred();
            var i = 0;
            var intervalId = setInterval(function() {
                var connectId = utils.getCookie("cpgdrive-cloud-drive-connect-id");
                if (connectId) {
                    // user authenticated and connect allowed
                    intervalId = clearInterval(intervalId);
                    process.resolve();
                } else {
                    var error = utils.getCookie("cpgdrive-cloud-drive-error");
                    if (error) {
                        intervalId = clearInterval(intervalId);
                        // XXX workaround for Google Drive's  access cancellation
                        if (error === "Access denied to Google Drive") {
                            process.reject(null);
                        } else {
                            process.reject(error);
                        }
                    } else if (authWindow && authWindow.closed) {
                        intervalId = clearInterval(intervalId);
                        utils.log("Authentication canceled.");
                        // reject w/o UI message
                        process.reject(null);
                    } else if (i > 310) {
                        // +10sec to ConnectService.INIT_COOKIE_EXPIRE
                        // if open more 5min - close it and treat as not authenticated/allowed
                        intervalId = clearInterval(intervalId);
                        process.reject("Authentication timeout.");
                    }
                }
                i++;
            }, 1000);
            return process.promise();
        };

        var connectInit = function(providerId, callbacks) {
            var request = $.ajax({
                type: "GET",
                url: prefixUrl + "/portal/rest/copygdrive/clone/init/" + providerId,
                dataType: "json",
            });

            initRequestDefaults(request, callbacks);
        };
        var initRequestDefaults = function(request, callbacks) {
            request.fail(function(jqXHR, textStatus, err) {
                if (callbacks.fail && jqXHR.status != 309) {
                    // check if response isn't JSON
                    var data;
                    try {
                        data = $.parseJSON(jqXHR.responseText);
                        if (typeof data == "string") {
                            // not JSON
                            data = jqXHR.responseText;
                        }
                    } catch (e) {
                        // not JSON
                        data = jqXHR.responseText;
                    }
                    // in err - textual portion of the HTTP status, such as "Not
                    // Found" or "Internal Server Error."
                    callbacks.fail(data, jqXHR.status, err);
                }
            });
            // hacking jQuery for statusCode handling
            var jQueryStatusCode = request.statusCode;
            request.statusCode = function(map) {
                var user502 = map[502];
                if (!user502 && callbacks.fail) {
                    map[502] = function() {
                        // treat 502 as request error also
                        callbacks.fail("Bad gateway", "error", 502);
                    };
                }
                return jQueryStatusCode(map);
            };
            request.done(function(data, textStatus, jqXHR) {
                if (callbacks.done) {
                    callbacks.done(data, jqXHR.status, textStatus);
                }
            });
            request.always(function(jqXHR, textStatus) {
                if (callbacks.always) {
                    callbacks.always(jqXHR.status, textStatus);
                }
            });
        };
        var initRequest = function(request) {
            var process = $.Deferred();

            // stuff in textStatus is less interesting: it can be "timeout",
            // "error", "abort", and "parsererror",
            // "success" or smth like that
            request.fail(function(jqXHR, textStatus, err) {
                if (jqXHR.status != 309) {
                    // check if response isn't JSON
                    var data;
                    try {
                        data = $.parseJSON(jqXHR.responseText);
                        if (typeof data == "string") {
                            // not JSON
                            data = jqXHR.responseText;
                        }
                    } catch (e) {
                        // not JSON
                        data = jqXHR.responseText;
                    }
                    // in err - textual portion of the HTTP status, such as "Not
                    // Found" or "Internal Server Error."
                    process.reject(data, jqXHR.status, err, jqXHR);
                }
            });
            // hacking jQuery for statusCode handling
            var jQueryStatusCode = request.statusCode;
            request.statusCode = function(map) {
                var user502 = map[502];
                if (!user502) {
                    map[502] = function() {
                        // treat 502 as request error also
                        process.fail("Bad gateway", 502, "error");
                    };
                }
                return jQueryStatusCode(map);
            };

            request.done(function(data, textStatus, jqXHR) {
                process.resolve(data, jqXHR.status, textStatus, jqXHR);
            });

            request.always(function(data, textStatus, errorThrown) {
                var status;
                if (data && data.status) {
                    status = data.status;
                } else if (errorThrown && errorThrown.status) {
                    status = errorThrown.status;
                } else {
                    status = 200;
                    // what else we could to do
                }
                process.always(status, textStatus);
            });

            // custom Promise target to provide an access to jqXHR object
            var processTarget = {
                request: request,
            };
            return process.promise(processTarget);
        };

        var connectCheck = function(checkUrl) {
            var process = $.Deferred();
            var serviceUrl = checkUrl;
            // if Accepted start Interval to wait for Created
            var intervalId = setInterval(function() {
                // use serviceUrl to check until 201/200 will be returned or an error
                var check = serviceGet(serviceUrl);
                check.done(function(state, status) {
                    if (status == "204") {
                        // No content - not a cloud drive or drive not connected, or not to this
                        // user. This also might mean an error as connect not active but the drive not
                        // connected.
                        process.reject("Drive not cloned. Check if no other clone process active and try again.");
                    } else if (state && state.serviceUrl) {
                        serviceUrl = state.serviceUrl;
                        if (status == "201" || status == "200") {
                            // created or ok - drive successfully connected or appears as already connected (by another request)
                            process.resolve(state);
                            utils.log("DONE: " + status + " " + state.drive.name + " cloned successfully.");
                        } else if (status == "202") {
                            // else inform progress and continue
                            process.notify(state);
                            utils.log(
                                "PROGRESS: " +
                                status +
                                " " +
                                state.drive.name +
                                " connectCheck progress " +
                                state.progress
                            );
                        } else {
                            // unexpected status, wait for created
                            utils.log("WARN: unexpected status in connectCheck:" + status);
                        }
                    } else {
                        utils.log("ERROR: " + status + " connectCheck return wrong state.");
                        var driveName;
                        if (state.drive && state.drive.name) {
                            driveName = state.drive.name;
                        } else {
                            driveName = "Cloud Drive";
                        }
                        process.reject("Cannot connect " + driveName + ". Server return wrong state.");
                    }
                });
                check.fail(function(state, error, errorText) {
                    utils.log("ERROR: Connect check error: " + error + ". " + JSON.stringify(state));
                    if (typeof state === "string") {
                        process.reject(state);
                    } else {
                        process.reject("Internal error: " + (state && state.error ? state.error : error + " " + errorText));
                    }
                });
            }, 3333);

            // finally clear interval
            process.always(function() {
                intervalId = clearInterval(intervalId);
            });

            return process.promise();
        };

        var serviceGet = function(url, data, contentType) {
            var request = $.ajax({
                async: true,
                type: "GET",
                url: url,
                dataType: "json",
                contentType: contentType ? contentType : undefined,
                data: data ? data : {},
            });
            return initRequest(request);
        };

        this.cloneprocess = function(process) {
            var driveName;
            var progress = 0;
            var hideTimeout;
            var stack_topright = {"dir1": "down", "dir2": "left", "firstpos1": 5, "firstpos2": 5};

            // pnotify notice
            var notice = $.pnotify({
                title : "Cloning in progress...",
                type : "info",
                icon : "picon picon-throbber",
                hide : false,
                closer : true,
                sticker : false,
                opacity : .75,
                shadow : false,
                nonblock : true,
                nonblock_opacity : .25,
                width : NOTICE_WIDTH
            });

            // show close button in 20s
            var removeNonblock = setTimeout(function() {
                notice.pnotify({
                    nonblock : false
                });
            }, 20000);

            var update = function() {
                var options = {
                };
                if (progress > 0) {
                    options.text = progress + "% complete.";
                }
                if (progress >= 75) {
                    options.title = "Almost Done...";
                }
                if (progress >= 100) {
                    options.title = driveName + " Cloned!";
                    options.type = "success";
                    options.hide = true;
                    options.closer = true;
                    options.sticker = false;
                    options.icon = "picon picon-task-complete";
                    options.opacity = 1;
                    options.shadow = true;
                    options.width = NOTICE_WIDTH;
                    // options.min_height = "300px";
                    options.nonblock = false;
                    // remove non-block
                }
                notice.pnotify(options);
            };

            process.progress(function(state) {
                // need update drive name
                update();
            });

            process.done(function (state) {
                if (state.drive && !state.error) {
                    driveName = state.drive.name;
                    if (hideTimeout) {
                        clearTimeout(hideTimeout);
                    }

                    // wait a bit for JCR/WCM readines
                    setTimeout(function () {
                        // update progress
                        progress = 100;
                        update();
                        refresh();
                        //driveMessage(state.drive);
                    }, 3000);
                } else {
                    process.fail(state.error);
                }
            });

            process.fail(function(message, title) {
                if (hideTimeout) {
                    clearTimeout(hideTimeout);
                }

                // when message undefined/null then process failure silently
                if (message) {
                    var options = {
                        text : message.error,
                        title : " Error occurred while cloning your drive!",
                        type : "error",
                        hide : true,
                        closer : true,
                        sticker : false,
                        icon : "picon picon-process-stop",
                        opacity : 1,
                        shadow : true,
                        width : NOTICE_WIDTH,
                        // remove non-block
                        nonblock : false
                    };
                    notice.pnotify(options);
                    refresh();
                } else {
                    var options = {
                        title : "Canceled",
                        type : "information",
                        hide : true,
                        delay : 1500,
                        closer : true,
                        sticker : false,
                        icon : "picon-dialog-information",
                        opacity : 1,
                        shadow : true,
                        width : NOTICE_WIDTH,
                        nonblock : true
                    };
                    notice.pnotify(options);
                }
            });
        };
        var refresh = function(forceRefresh) {
            if (forceRefresh) {
                // refresh view w/ popup
                $("a.refreshIcon i.uiIconRefresh").click();
            } else {
                // don't refresh if user actions active or if file view active
                if ($("div#UIDocumentInfo:visible").length > 0) {
                    if ($("div#UIPopupWindow:visible, div#UIRenameWindowPopup, span.loading").length == 0) {
                        // refresh view w/o popup
                        $("#ECMContextMenu a[exo\\:attr='RefreshView'] i").click();
                    }
                }
            }
        };
    }
    var clonedDrive = new ClonedDrive();

    return clonedDrive;
})($, cloudDriveUtils, cloudDrives);