/**
 * 
 */
var createTaskApp = angular.module('createTaskApp', []);

createTaskApp.controller('CreateTaskFormController',['$scope',function($scope){
	$scope.contentImage="/assets/images/add-file-512x512.png";
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
		$scope.submitCreateTaskForm = function(){
			document.create_task_form.submit();
		}
}]);

