angular.module('viewTaskApp', [ 'ngRoute', 'ngResource' ]).service('Task',
		[ '$resource', function($resource) {
			return $resource('/api/task/:taskId');
		} ]).controller(
		"viewTaskController",
		[ '$scope', '$location', '$timeout', 'Task',
				function($scope, $location, $timeout, Task) {
					var taskId = $location.search().taskId;
					$scope.refreshTask = function() {

						console.debug('taskId: ' + taskId);
						Task.get({
							'taskId' : taskId
						}, function(task) {
							$scope.task = task;
							if ($scope.task.outputImageUrl === undefined) {
								console.debug(task);
								$scope.task.outputImageUrl='/assets/images/painting.png';
								$timeout($scope.refreshTask, 3000);
							}
						});

					}

					$timeout($scope.refreshTask, 100);
				} ]);
