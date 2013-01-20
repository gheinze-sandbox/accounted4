'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('a4App.services', []).
  value('version', '0.1');



angular.module('a4App.services').factory('AmortizationService', function($http) {
    
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
                            data[i].date = new Date(data[i].paymentDate[0], data[i].paymentDate[1], data[i].paymentDate[2]);
                        }
                        onSuccess(data);
                    }
                
                ).error(
                        
                    function(data, status, headers, config) {
                        alert("Amortization service failed to return amortization schedule.");
                    }
                
                );
            
        }
    
    
    };

});

