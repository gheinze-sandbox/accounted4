'use strict';


/* Controllers */


function MyCtrl1() {}
MyCtrl1.$inject = [];


function MyCtrl2() {
}
MyCtrl2.$inject = [];




function AmortizationCalculatorCtrl($scope, $filter, AmortizationService) {
    
    var formModel = {};
    
    $scope.formModel = formModel;
    
    var today = new Date();

    
    // HTML5 Date input control expects ISO date format for value: YYYY-MM-DD
    formModel.startDate = $filter('date')(today, 'yyyy-MM-dd');


    // The adjustment date is recalculated each time the startDate is modified
    formModel.setAdjustmentDate = function() {
        
        var newStartDate = new Date(formModel.startDate);

        var year = newStartDate.getFullYear();
        var month = newStartDate.getMonth();
        var dayOfMonth = newStartDate.getDate();

        var newAdjustmentDate;
        
        // Always adjust to the following 1st or 15th of the month if not already on the 1st or 15th
        if (dayOfMonth > 15) {
            newAdjustmentDate = new Date(year, month + 1, 1);
        } else if (dayOfMonth > 1 && dayOfMonth < 15) {
            newAdjustmentDate = new Date(year, month, 15);
        } else {
            newAdjustmentDate = new Date(formModel.startDate);
        }

        formModel.adjustmentDate = $filter('date')(newAdjustmentDate, 'yyyy-MM-dd');

    };


    // Trigger an initial adjustment date update
    formModel.setAdjustmentDate();
    
    
    formModel.termInMonths = 12;         // default 1 year term
    formModel.maxTermInMonths = 12 * 30; // mas 30 year term
 
    formModel.interestOnly = true;
    
    formModel.amortizationPeriodYears = 20;
    formModel.maxAmortizationYears = 30;
    
    formModel.amortizationPeriodMonths = 0;
    
    formModel.compoundingPeriods = [
        {name:'monthly', periodsPerYear:'12', comment:'American mortgage default'},
        {name:'semi-annually', periodsPerYear:'2', comment:'Canadian mortgage default'},
        {name:'annually', periodsPerYear:'1', comment:''}
    ];

    formModel.compoundingPeriod = formModel.compoundingPeriods[1];

    formModel.amount = "20000";
    
    formModel.interestRate = 10;         // default 10%
    formModel.maxInterestRate = 25;
    formModel.interestRateStep = 0.25;

    formModel.monthlyPayment = "";
    
    
    formModel.schedule = [];
    formModel.scheduleShown = false;
    
    
    formModel.calculateMonthlyPayment = function() {
        
        AmortizationService.monthlyPayment(
                formModel.extractData()
               ,function(monthlyAmount) {
                   formModel.monthlyPayment = monthlyAmount;
               } );
        
    };


    formModel.generateAmSchedule = function() {
        AmortizationService.amSchedule(
                formModel.extractData()
               ,function(data) {
                    formModel.schedule = data;
                    formModel.scheduleShown = true;
               } );
    };



    formModel.dismissSchedule = function() {
        formModel.scheduleShown = false;
        formModel.schedule = [];
    };


    // Generate an object representing the form values
    formModel.extractData = function() {

        var terms = {};
        
        terms.loanAmount = formModel.amount;
        terms.startDate = formModel.startDate;
        terms.adjustmentDate = formModel.adjustmentDate;
        terms.termInMonths = formModel.termInMonths;
        terms.interestOnly = formModel.interestOnly;
        terms.amortizationPeriodMonths = formModel.amortizationPeriodYears * 12 + formModel.amortizationPeriodMonths;
        terms.compoundingPeriodsPerYear = formModel.compoundingPeriod.periodsPerYear;
        terms.interestRate = formModel.interestRate;
        
        terms.regularPayment = formModel.monthlyPayment.length > 0 ? formModel.monthlyPayment : "0";

        
        return terms;
        
    };

}


// Explicitly list the named services injected into the controller to avoid future minification issues
AmortizationCalculatorCtrl.$inject = ['$scope', '$filter', 'AmortizationService'];