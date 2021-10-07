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
                                clone.done(function (state, status) {
                                    utils.log("CLone requested: " + status + ". ");
                                    if (state) {
                                        if (status == 201) {
                                            utils.log("DONE: " + provider.name + " successfully Cloned.");
                                            self.contextDrive = state.drive;
                                            process.resolve(state);
                                        } else if (status == 202) {
                                           var check = cloneCheck(state.serviceUrl);
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

        var cloneCheck = function(checkUrl) {
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

        this.cloneState = function(checkUrl, docsUrl, docsOnclick) {
            var task;
            if (taskStore) {
                // add check task to get user notified in case of leaving this
                // page
                task = "clonedDrive.cloneState(\"" + checkUrl + "\", \"" + docsUrl + "\", \"" + docsOnclick + "\");";
                taskStore.add(task);
            } else {
                utils.log("Tasks not defined");
            }
            var notice = $.pnotify({opacity:0});

            var state = cloneCheck(checkUrl);
            state.done(function(state) {
                var message;
                if (docsUrl) {
                    message = '<div>Find your drive in <a href="' + docsUrl + '"';
                    if (docsOnclick) {
                        message += " onclick='" + docsOnclick + "'";
                    }
                    message += "'>Space Documents</div>";
                } else {
                    message = "Find your drive in Space Documents";
                }
                notice.pnotify({
                    title : "Your " + state.drive.name + " cloned!",
                    type : "success",
                    text : message,
                    icon : "picon picon-task-complete",
                    hide : true,
                    closer : true,
                    sticker : false,
                    opacity : 1,
                    shadow : true,
                    width : $.pnotify.defaults.width
                });
                //driveMessage(state.drive);
            });
            state.progress(function (state) {
                var processLinks = state.progress >= 100 ? "\n Processing Links..." :"";
                notice.pnotify({
                    title : "Cloning Your " + state.drive.name + processLinks,
                    text: state.progress > 100 ? "100" : state.progress + "% complete.",
                    type : "info",
                    icon : "picon picon-throbber",
                    hide : false,
                    closer : true,
                    sticker : false,
                    opacity : .90,
                    shadow : false,
                    nonblock : true,
                    nonblock_opacity : .25,
                    width : NOTICE_WIDTH
                });
            });
            state.fail(function(state) {
                var message;
                if (state.drive && state.drive.name) {
                    message = "Error cloning your " + state.drive.name;
                } else {
                    message = "Error cloning your drive";
                }
                notice.pnotify({
                    title : message,
                    text : state.error,
                    type : "error",
                    hide : true,
                    closer : true,
                    sticker : false,
                    icon : 'picon picon-dialog-error',
                    opacity : 1,
                    shadow : true,
                    width : $.pnotify.defaults.width
                });
            });
            state.always(function() {
                if (task) {
                    taskStore.remove(task);
                }
            });
        };

        var spaceDocumentsLink = function() {
            var $link = $("a.refreshIcon");
            if ($link.length > 0) {
                return $link.attr("href");
            }
        };

        this.cloneProcess = function(process) {
            var linksProcessFinished = false
            var task;
            var driveName;
            var progress = 0;
            var hideTimeout;
            var stack_topright = {"dir1": "down", "dir2": "left", "firstpos1": 5, "firstpos2": 5};

            // pnotify notice
            var notice = $.pnotify({
                title : "Waiting for authentication...",
                type : "info",
                icon : "picon picon-throbber",
                hide : false,
                closer : true,
                sticker : false,
                opacity : .90,
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
                progress = progress > 100 ? 100 : progress
                if (progress > 0) {
                    options.text = progress + "% complete.";
                }
                if (progress >= 75) {
                    options.title = "Almost Done...";
                }
                if (progress >= 100) {
                    var icon = linksProcessFinished == true ? "picon-task-complete" : "picon-throbber"
                    var processLinks = progress >= 100 ? "\n Processing Links..." :"";
                    options.title = driveName + " Cloned!" + processLinks;
                    options.type = "success";
                    options.hide = true;
                    options.closer = true;
                    options.sticker = false;
                    options.icon = "picon " + icon;
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
                if (!task) {
                    progress = state.progress;
                    if (progress > 0) {
                        driveName = state.drive.name;

                        notice.pnotify({
                            title : "Cloning Your " + driveName,
                            text: progress > 100 ? "100" : progress + "% complete."
                        });

                        // hide title in 5sec
                        hideTimeout = setTimeout(function() {
                            notice.pnotify({
                                title : false,
                                width : "200px"
                            });
                        }, 5000);

                        // add as tasks also
                        if (taskStore) {
                            var docsUrl = ", \"" + location + "\"";
                            var docsOnclick = spaceDocumentsLink();
                            docsOnclick = docsOnclick ? ", \"" + docsOnclick + "\"" : "";
                            task = "clonedDrive.cloneState(\"" + state.serviceUrl + "\"" + docsOnclick + ");";
                            taskStore.add(task);
                        } else {
                            utils.log("Tasks not defined");
                        }
                    }

                } else {
                    driveName = state.drive.name;
                    progress = state.progress;
                    linksProcessFinished = state.drive.linksProcessed;
                }
                update();
            });

            process.done(function (state) {
                if (state.drive && !state.error) {
                    driveName = state.drive.name;
                    linksProcessFinished = state.drive.linksProcessed;
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
                }
            });
            process.always(function() {
                if (task) {
                    taskStore.remove(task);
                }
            });

            process.fail(function(message, title) {
                if (hideTimeout) {
                    clearTimeout(hideTimeout);
                }

                // when message undefined/null then process failure silently
                if (message) {
                    var options = {
                        text : message,
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
    function taskStore() {
        var COOKIE_NAME = "cpgdrive-tasks.exoplatform.org";
        var loaded = false;

        /**
         * Add on-load callback to the window object.
         */
        var onLoad = function (fn) {
            if (window.addEventListener) {
                window.addEventListener("load", fn, false); // W3C
            } else if (window.attachEvent) {
                window.attachEvent("onload", fn); // IE8
            } else {
                if (window.onload) {
                    var currOnLoad = window.onload;
                    var newOnLoad = function () {
                        if (currOnLoad)
                            currOnLoad();
                        fn();
                    };
                    window.onload = newOnLoad;
                } else {
                    window.onload = fn;
                }
            }
        };

        var store = function (tasks) {
            utils.setCookie(COOKIE_NAME, tasks, 20 * 60 * 1000); // 20min
        };

        var removeTask = function (task) {
            var pcookie = utils.getCookie(COOKIE_NAME);
            if (pcookie && pcookie.length > 0) {
                var updated = "";
                var existing = pcookie.split("~");
                for (var i = 0; i < existing.length; i++) {
                    var t = existing[i];
                    if (t != task) {
                        updated += updated.length > 0 ? "~" + t : t;
                    }
                }
                store(updated);
            }
        };

        var addTask = function (task) {
            var pcookie = utils.getCookie(COOKIE_NAME);
            if (pcookie) {
                if (pcookie.indexOf(task) < 0) {
                    var tasks = pcookie.length > 0 ? pcookie + "~" + task : task;
                    store(tasks);
                }
            } else {
                store(task);
            }
        };

        /**
         * Load stored tasks.
         */
        var load = function () {
            // load once per page
            if (loaded)
                return;

            // read cookie and eval each stored code
            var pcookie = utils.getCookie(COOKIE_NAME);
            if (pcookie && pcookie.length > 0) {
                try {
                    var tasks = pcookie.split("~");
                    for (var i = 0; i < tasks.length; i++) {
                        var task = tasks[i];
                        try {
                            removeTask(task);
                            utils.log("Loading task [" + task + "]");
                            eval(task);
                        } catch (e) {
                            utils.log("Error evaluating task: " + task + ":" + e + ". Skipped.");
                        }
                    }
                } finally {
                    loaded = true;
                    utils.log("Tasks loaded.");
                }
            }
        };

        /**
         * Register task in store.
         */
        this.add = function (task) {
            if (task) {
                addTask(task);
            } else {
                utils.log("not valid task (code is not defined)");
            }
        };

        /**
         * Remove task from the store.
         */
        this.remove = function (task) {
            removeTask(task);
        };


        $(function () {
            try {
                setTimeout(function () {
                    utils.log("Loading deffered tasks");
                    load();
                }, 4000);
            } catch (e) {
                utils.log("Error loading tasks", e);
            }
        });
    }
    var taskStore = new taskStore();
    var clonedDrive = new ClonedDrive();

    return clonedDrive;
})($, cloudDriveUtils, cloudDrives);