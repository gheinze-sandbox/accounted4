'use strict';


function PartyCtrl($scope, $filter) {
    
    // --------------------------------------
    // Model containing the values stored in the party form
    // --------------------------------------

    var formModel = {};
    
    $scope.formModel = formModel;


    // --------------------------------------
    // Constants used by the form
    // --------------------------------------
    formModel.ORGANIZATION_TYPE = "organization";
    formModel.INDIVIDUAL_TYPE = "individual";


    // --------------------------------------
    // Utility Functions
    // --------------------------------------
    
    
    
    // Populate the form based on provided form values
    formModel.setFormValues = function(formValues) {
        
        formModel.partyType = formValues.partyType;
        formModel.organizationName = formValues.organizationName;
        formModel.lastName = formValues.lastName;
        formModel.firstName = formValues.firstName;
        formModel.displayName = formValues.displayName;

    };
    
    
    // Generate an object representing the form values
    formModel.extractData = function() {

        var formValues = {};
        
        formValues.partyType = formModel.partyType;

        return formValues;
        
    };


    
    // --------------------------------------
    // Form handlers
    // --------------------------------------
    
    formModel.isIndividual = function() {
        var result = (formModel.partyType === formModel.INDIVIDUAL_TYPE);
        return result;
    };

    formModel.isOrganization = function() {
        var result = (formModel.partyType === formModel.ORGANIZATION_TYPE);
        return result;
    };
    
    formModel.nameChanged = function() {
        if (formModel.partyType === formModel.ORGANIZATION_TYPE) {
            formModel.displayName = formModel.organizationName;
        } else {
            var lastName = (formModel.lastName) ? formModel.lastName : "";
            var separator = (formModel.lastName) && (formModel.firstName) ? ", " : "";
            var firstName = (formModel.firstName) ? formModel.firstName : "";
            formModel.displayName = lastName + separator + firstName; 
        }
    };


    // --------------------------------------
    // Form Initialization
    // --------------------------------------
    
    // Create an object representing the initial, default state of the form
    var initialFormValues = {};
    
    initialFormValues.partyType = formModel.ORGANIZATION_TYPE;
    initialFormValues.organizationName = "";
    initialFormValues.lastName = "";
    initialFormValues.firstName = "";
    initialFormValues.displayName = "";
    
    formModel.setFormValues(initialFormValues);
    

}


// Explicitly list the named services injected into the controller to avoid future minification issues
PartyCtrl.$inject = ['$scope', '$filter'];