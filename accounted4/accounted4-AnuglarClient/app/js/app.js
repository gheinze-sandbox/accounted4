'use strict';


// Declare app level module which depends on filters, and services
angular.module('a4App', ['a4App.filters', 'a4App.services', 'a4App.directives', 'ngResource', 'ui']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/view1', {templateUrl: 'partials/partial1.html', controller: MyCtrl1});
    $routeProvider.when('/view2', {templateUrl: 'partials/partial2.html', controller: MyCtrl2});
    $routeProvider.when('/amortizationCalculator', {templateUrl: 'partials/amortizationCalculator.html', controller: AmortizationCalculatorCtrl});
    $routeProvider.when('/party', {templateUrl: 'partials/party.html', controller: PartyCtrl});
    $routeProvider.otherwise({redirectTo: '/view1'});
  }]);
