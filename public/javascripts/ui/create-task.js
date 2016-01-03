/**
 * 
 */
var createTaskApp = angular.module('createTaskApp', []);

createTaskApp.controller('CreateTaskFormController',['$scope','$http',function($scope,$http){
	$scope.contentImage="/assets/images/add-file-512x512.png";
	// load existed tasks from localstorage
	$scope.myTasks = JSON.parse(localStorage['myTasks'] || '[]').slice(0,10);
	$scope.clickContentImage = function(){
		console.log('clicked content image');
		document.getElementById("content-image-input").click();
	};
	
	$scope.setFile = function(element) {
		  $scope.currentFile = element.files[0];
		   var reader = new FileReader();

		  reader.onload = function(event) {
		    $scope.contentImage = event.target.result
		    $scope.$apply()

		  }
		  // when the file is read it triggers the onload event above.
		  reader.readAsDataURL(element.files[0]);
		};
		
		var storeTaskOnLocal = function(task){
			if(localStorage){
				var taskList = JSON.parse(localStorage['myTasks'] || '[]');
				taskList.push(task);
				localStorage['myTasks']=JSON.stringify(taskList);
			}
		}
		$scope.submitCreateTaskForm = function(){
			var formData = new FormData(document.create_task_form);
			$http.post("/service/task/create",formData,{
				withCredentials:false,
				headers:{
					'Content-Type':undefined
				},
				transformRequest:angular.identify,
				responseType:"application/json"
			}).success(function(response,status,headers,config){
				console.log(response);
				storeTaskOnLocal(response);
				
				// redirect to view task
				var viewTaskUrl="/assets/ui/view-task.html#?taskId="+response._id;
				window.location=viewTaskUrl;
			}).error(function(error,status,headers,config){
				console.error(error);
				
			});
		}
}]);

