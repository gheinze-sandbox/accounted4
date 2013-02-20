'use strict';


/* Controllers */


function MyCtrl1() {}
MyCtrl1.$inject = [];


function MyCtrl2() {
}
MyCtrl2.$inject = [];




function AmortizationCalculatorCtrl($scope, $filter, AmortizationService) {
    
    // --------------------------------------
    // Model containing the values stored in the form
    // --------------------------------------

    var formModel = {};
    
    $scope.formModel = formModel;


    // --------------------------------------
    // Constants used by the form
    // --------------------------------------
    
    formModel.dateFormat = /^(19\d{2})|(20\d{2})-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-(0?[1-9]|[12][0-9]|3[01])$/;
    formModel.maxTermInMonths = 12 * 30; // max 30 year term
    formModel.maxAmortizationYears = 30;
    
    formModel.compoundingPeriods = [
        {name:'monthly', periodsPerYear:'12', comment:'American mortgage default'},
        {name:'semi-annually', periodsPerYear:'2', comment:'Canadian mortgage default'},
        {name:'annually', periodsPerYear:'1', comment:''}
    ];
    
    formModel.maxInterestRate = 25;
    formModel.interestRateStep = 0.25;

    formModel.datePickerOptions = {
         dateFormat: 'yy-M-dd'
        ,changeMonth: true
        ,changeYear: true
        ,buttonImage: 'img/office-calendar.png'
        ,buttonImageOnly: false
        ,showOn: "both" // "button" or "focus" or "both"
    };

    // --------------------------------------
    // Utility Functions
    // --------------------------------------
    
    // Given a Date input, find the nearest 1st or 15th of the month
    // on or after the given date
    var calculateAdjustmentDate = function (baseDate) {
        
        if (undefined === baseDate || null === baseDate ||  ! (baseDate instanceof Date) ) {
            baseDate = new Date();
        }
        
        var year = baseDate.getFullYear();
        var month = baseDate.getMonth();
        var dayOfMonth = baseDate.getDate();

        if (dayOfMonth > 15) {
            month = month + 1;
            dayOfMonth = 1;
        } else if (dayOfMonth > 1 && dayOfMonth < 15) {
            dayOfMonth = 15;
        }
        
        return new Date(year, month, dayOfMonth);

    };
    
    
    // Populate the form based on provided form values
    formModel.setFormValues = function(formValues) {
        
        formModel.startDate = formValues.startDate;
        formModel.adjustmentDate = formValues.adjustmentDate;
        formModel.termInMonths = formValues.termInMonths;
        formModel.interestOnly = formValues.interestOnly;
        formModel.amortizationPeriodYears = formValues.amortizationPeriodYears;
        formModel.amortizationPeriodMonths = formValues.amortizationPeriodMonths;
        formModel.compoundingPeriod = formValues.compoundingPeriod;
        formModel.amount = formValues.amount;
        formModel.interestRate = formValues.interestRate;
        formModel.monthlyPayment = formValues.monthlyPayment;

    };
    
    
    // Generate an object representing the form values
    formModel.extractData = function() {

        var terms = {};
        
        terms.startDate = formModel.startDate;
        terms.adjustmentDate = formModel.adjustmentDate;
        terms.termInMonths = formModel.termInMonths;
        terms.interestOnly = formModel.interestOnly;
        terms.amortizationPeriodMonths = formModel.amortizationPeriodYears * 12 + formModel.amortizationPeriodMonths;
        terms.compoundingPeriodsPerYear = formModel.compoundingPeriod.periodsPerYear;
        terms.loanAmount = formModel.amount;
        terms.interestRate = formModel.interestRate;
        
        terms.regularPayment = formModel.monthlyPayment.length > 0 ? formModel.monthlyPayment : "0";

        return terms;
        
    };


    
    // --------------------------------------
    // Form handlers
    // --------------------------------------
    
    // The adjustment date is recalculated each time the startDate is modified
    formModel.setAdjustmentDate = function() {
        formModel.adjustmentDate = calculateAdjustmentDate(formModel.startDate);
    };


    formModel.calculateMonthlyPayment = function() {
        
        AmortizationService.monthlyPayment(
                formModel.extractData()
               ,function(monthlyAmount) {
                   formModel.monthlyPayment = monthlyAmount;
               } );
        
    };



    // --------------------------------------
    // Form Initialization
    // --------------------------------------
    
    // Create an object representing the initial, default state of the form
    var initialFormValues = {};
    initialFormValues.startDate = new Date();
    initialFormValues.adjustmentDate = calculateAdjustmentDate(initialFormValues.startDate);
    initialFormValues.termInMonths = 12;  // default 1 year term
    initialFormValues.interestOnly = true;
    initialFormValues.amortizationPeriodYears = 20;
    initialFormValues.amortizationPeriodMonths = 0;
    initialFormValues.compoundingPeriod = formModel.compoundingPeriods[1];
    initialFormValues.amount = "20000";
    initialFormValues.interestRate = 10; // default 10%
    initialFormValues.monthlyPayment = "";

    
    formModel.setFormValues(initialFormValues);
    
    
    // --------------------------------------
    // Schedule pop-up setup
    // --------------------------------------
    
    formModel.schedule = [];
    formModel.scheduleShown = false;
    
    
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


}


// Explicitly list the named services injected into the controller to avoid future minification issues
AmortizationCalculatorCtrl.$inject = ['$scope', '$filter', 'AmortizationService'];