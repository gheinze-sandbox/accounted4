'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('a4App.services', []).
  value('version', '0.1');



angular.module('a4App.services').factory('AmortizationService', function($http, $window) {
    
    return {
        
        monthlyPayment : function(amAttributes, onSuccess) {
            
            var httpPostConfig = {
                method: 'POST'
                        , url: 'http://localhost:8084/accounted4-midtier/amortization/monthlyPayment'
                        , data: amAttributes
            };


            $http(httpPostConfig)
                    .success(function(data, status, headers, config) { onSuccess(data.amount); })
                    .error(function(data, status, headers, config) {
                        alert("Amortization service failed to return a monthly payment amount.");
            });
            
        },
    
    
        amSchedule : function(amAttributes, onSuccess) {
            
            var httpPostConfig = {
                method: 'POST'
                        , url: 'http://localhost:8084/accounted4-midtier/amortization/schedule.json'
                        , data: amAttributes
            };

            $http(httpPostConfig).success(
                    
                    function(data, status, headers, config) {
                        for (var i = 0; i < data.length; i++) {
                            // The payment date is a String in "2013-12-31" format
                            // Generate an actual Javascript date object based on the parts
                            var dateParts = data[i].paymentDate.split('-');
                            data[i].date = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);
                        }
                        onSuccess(data);
                    }
                
                ).error(
                        
                    function(data, status, headers, config) {
                        alert("Amortization service failed to return amortization schedule.");
                    }
                
                );
            
        },
    

        // For pdf functionality, post the attributes to the server. Then open
        // a new browser window requesting the doc id returned by the previous post.
        amSchedulePdf : function(amAttributes) {
            
            var httpPostConfig = {
                method: 'POST'
                        , url: 'http://localhost:8084/accounted4-midtier/amortization/prepareSchedule'
                        , data: amAttributes
            };

            $http(httpPostConfig).success(
                    
                    function(data, status, headers, config) {
                        var url = "http://localhost:8084/accounted4-midtier/amortization/showSchedule/pdf/" + data.id;
                        $window.open(url);
                    }
                
                ).error(
                        
                    function(data, status, headers, config) {
                        alert("Amortization service failed to return amortization schedule.");
                    }
                
                );
            
        }
                
                
                
    };

});

