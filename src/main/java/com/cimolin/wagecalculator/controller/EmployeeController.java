package com.cimolin.wagecalculator.controller;

import com.cimolin.wagecalculator.entity.Employee;
import com.cimolin.wagecalculator.controller.util.JsfUtil;
import com.cimolin.wagecalculator.controller.util.JsfUtil.PersistAction;

import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;

@Named("employeeController")
@SessionScoped
public class EmployeeController implements Serializable {

    @EJB
    private EmployeeFacade ejbFacade;
    private List<Employee> items = null;
    private Employee selected;
    private Integer dependents;

    public EmployeeController() {
    }

    public Employee getSelected() {
        return selected;
    }

    public void setSelected(Employee selected) {
        this.selected = selected;
    }
    
     /**
     * Returns the INSS aliquot based on the employee's gross income.
     * @param wage The employee's wage
     * @return aliquot - percentage value, will deduct from the gross wage.
     */
    public Double getINSSAliquot(Double wage){
        Double aliquot = 0d;
        
        if(wage <= 1693.72){
            aliquot = 8d;
        } else if(wage <= 2822.9){
            aliquot = 9d;
        } else if(wage <= 5645.8){
            aliquot = 11d;
        }
        //If wage happens to be more than the above table, the INSS
        //value becomes a fixed value in Reais (R$).
        
        return aliquot;
    }
    
    /**
     * Returns the INSS deduction that will subtract from the gross income.
     * @param netWage the current wage to be calculated
     * @return INSS deduction in Reais (R$)
     */
    public Double getINSSDeduction(Double netWage){
        Double deduction;
        
        deduction = getINSSAliquot(netWage) == 0d ? 621.04d : 
                getINSSAliquot(netWage)/100 * netWage;
        
        if(dependents > 0){
            if(netWage < 877.68){
                deduction -= 45d;
            } else if(netWage < 1319.18){
                deduction -= 31.71d;
            }
        }
        //If method returns zero, that means the wage higher than the
        //maximum INSS value table. So it is fixedly valued at 621.04 R$
        return JsfUtil.Round(deduction);
    }
    
    /**
     * Returns the IRRF aliquot to deduct from the net wage already
     * deducted with the INSS value.
     * @param wage The employee's wage
     * @return IRRF aliquot
     */
    public Double getIRRFAliquot(Double wage){
        Double aliquot = 0d;
        
        if(wage <= 1903.98){
            aliquot = 0d;
        } else if(wage <= 2826.65){
            aliquot = 7.5d;
        } else if(wage <= 3751.05){
            aliquot = 15d;
        } else if(wage <= 4664.68){
            aliquot = 22.5d;
        } else {
            aliquot = 27.5d;
        } 
        
        return aliquot;
    }
    
    /**
     * returns the IRRF value already deducted by the INSS tribute value.
     * @param netWage
     * @return 
     */
    public Double getIRRFDeduction(Double netWage){
        Double aliquot = getIRRFAliquot(netWage);
        Double deduction;
        Double counter = 0d;
        
        if(aliquot <= 7.5){
            counter = 142.8;
        } else if (aliquot <= 15){
            counter = 354.8;
        } else if (aliquot <= 22.5){
            counter = 636.13;
        } else {
            counter = 869.36;
        }
        
        deduction = (netWage - getINSSDeduction(netWage)) * aliquot/100 - counter;
        
        return JsfUtil.Round(Math.max(deduction,0));
    }
    
    /***
     * Returns the net wage, deducting everything from INSS to IRRF.
     * @param grossWage The wage of the employee
     * @param dependents The number of dependents
     * @return net wage value in Reais (R$)
     */
    public Double getNetWage(Double grossWage, Integer dependents){
        Double deduction;
        this.dependents = dependents;
        
        deduction = getINSSDeduction(grossWage) + getIRRFDeduction(grossWage);
        
        return JsfUtil.Round(Math.max(grossWage - deduction, 0));
    }

    private EmployeeFacade getFacade() {
        return ejbFacade;
    }

    public Employee prepareCreate() {
        selected = new Employee();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("/Bundle").getString("EmployeeCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/Bundle").getString("EmployeeUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/Bundle").getString("EmployeeDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<Employee> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            }
        }
    }

    public Employee getEmployee(java.lang.Long id) {
        return getFacade().find(id);
    }
    
    public String getMoneyString(Double value){
        return JsfUtil.Money(value);
    }

}
