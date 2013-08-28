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
//        ,buttonText: '<i class=icon-calendar></i>'
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
    
    // JAXB unmarshalling into Joda date expects YYYY-MM-DD format string
    var convertDateToJodaString = function(inDate) {
        
        var separator = "-";
        
        var year = inDate.getFullYear().toString();
        
        // Javascript month is 0-based, change it to 1-based, add a leading "0"
        // but then only take the last two digits ie "01" - > "01", "012" -> "12"
        var month = "0" + (inDate.getMonth() + 1).toString();
        month = month.substr(month.length - 2);
        
        var day = "0" + inDate.getDate();
        day = day.substr(day.length - 2);
        
        return year + separator + month + separator + day;
        
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
        
        terms.startDate = convertDateToJodaString(formModel.startDate);
        terms.adjustmentDate = convertDateToJodaString(formModel.adjustmentDate);
                  
        terms.termInMonths = formModel.termInMonths;
        terms.interestOnly = formModel.interestOnly;
        terms.amortizationPeriodMonths = formModel.amortizationPeriodYears * 12 + formModel.amortizationPeriodMonths;
        terms.compoundingPeriodsPerYear = formModel.compoundingPeriod.periodsPerYear;
        terms.loanAmount = formModel.amount.toString();
        terms.interestRate = formModel.interestRate;
        
        // The regularPayment is not necessarily calculated (may be blank)
        // If empty, setting it to "0" will force the calculator increase to the minimum payment
        terms.regularPayment = (typeof formModel.monthlyPayment === 'string') ? "0" : formModel.monthlyPayment.toString();

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
    initialFormValues.amount = 20000;
    initialFormValues.interestRate = 10; // default 10%
    initialFormValues.monthlyPayment = "";

    
    formModel.setFormValues(initialFormValues);
    
    
    // --------------------------------------
    // Schedule pop-up setup
    // --------------------------------------

    // maintain state on visibility of schedule
    formModel.schedule = [];
    formModel.scheduleShown = false;
    
    
    // Amortization schedule dialog generation
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


    // Amortization schedule as a pdf document
    formModel.generateAmSchedulePdf = function() {
        AmortizationService.amSchedulePdf( formModel.extractData() );
    };


}


// Explicitly list the named services injected into the controller to avoid future minification issues
AmortizationCalculatorCtrl.$inject = ['$scope', '$filter', 'AmortizationService'];