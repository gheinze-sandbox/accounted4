package com.accounted4.midtier.controller;


import com.accounted4.midtier.service.AmortizationService;
import com.accounted4.money.Money;
import com.accounted4.money.loan.AmortizationAttributes;
import com.accounted4.money.loan.ScheduledPayment;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Sample controller for handling "rest" requests with auto marshalling/unmarshalling of requests and responses (json
 * and xml).
 *
 * @author Glenn Heinze <glenn@gheinze.com>
 */
@Controller
public class HomeController {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private AmortizationService amortizationService;


    /**
     * Helper class to allow the automarshalling of a List of beans into an XML format.
     *
     * @param <T>
     */
    @XmlRootElement(name = "List")
    @XmlSeeAlso(HomeController.User.class)
    public static class JaxbList<T> {

        protected List<T> list;


        public JaxbList() {
        }


        public JaxbList(List<T> list) {
            this.list = list;
        }


        @XmlElement(name = "Item")
        public List<T> getList() {
            return list;
        }

    }


    /**
     * Hack sample demonstrating querying a db to get a list, then automarshal the
     * list as a json response. Push db work into a service ...
     * @param id
     * @return 
     */
    @RequestMapping(value = "/users/{id}.json", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<User> getUserJson(@PathVariable Long id) {
        //return new User(id, "John Doe", "display name");
        String query = "SELECT name, display_name FROM user_account";
        SqlParameterSource namedParameters = new MapSqlParameterSource();

        List<User> users = jdbcTemplate.query(query, new SqlParameterSource() {
            @Override
            public boolean hasValue(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public Object getValue(String string) throws IllegalArgumentException {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public int getSqlType(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public String getTypeName(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


        }, new UserMapper());

        return users;

    }


    /**
     * Hack sample demonstrating querying a db to get a list, then automarshal the
     * list as a xml response. Push db work into a service ...
     * @param id
     * @return 
     */
    @RequestMapping(value = "/users/{id}.xml", method = RequestMethod.GET, produces = "application/xml")
    @ResponseBody
    public JaxbList<HomeController.User> getUserXml(@PathVariable Long id) {

        String query = "SELECT name, display_name FROM user_account";
        SqlParameterSource namedParameters = new MapSqlParameterSource();

        List<User> users = jdbcTemplate.query(query, new SqlParameterSource() {
            @Override
            public boolean hasValue(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public Object getValue(String string) throws IllegalArgumentException {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public int getSqlType(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


            @Override
            public String getTypeName(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


        }, new UserMapper());

        return new JaxbList<>(users);

        // return new User(id, "John Doe", "displayName");

    }


    private static final class UserMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet resultset, int rowNum) throws SQLException {
            User user = new User();
            user.setId(7L);
            user.setName(resultset.getString("name"));
            user.setDisplayName(resultset.getString("display_name"));

            return user;
        }


    }


    @XmlRootElement
    public static class User {

        private Long id;
        private String name;
        private String displayName;


        public User() {
        }


        public User(Long id, String name, String displayName) {
            this.id = id;
            this.name = name;
            this.displayName = displayName;
        }


        public Long getId() {
            return id;
        }


        public void setId(Long id) {
            this.id = id;
        }


        public String getName() {
            return name;
        }


        public void setName(String name) {
            this.name = name;
        }


        public String getDisplayName() {
            return displayName;
        }


        public void setDisplayName(String name) {
            this.displayName = name;
        }


    }


    public static class Term {

        public String value;
        public String value2;


        public void setValue(String val) {
            this.value = val;
        }


        public void setValue2(String val) {
            this.value2 = val;
        }


    }

// ==============================================

    // Very fussy: all fields required, no tabs/line breaks
    // http://localhost:8080/accounted4-midtier/amortizationSchedule.json
    // Content-Type: application/json
    // {"loanAmount":"20000.00", "regularPayment":"200","startDate":"2013-01-05","adjustmentDate":"2013-01-15","termInMonths":"12","interestOnly":"true","amortizationPeriodMonths":"20","compoundingPeriodsPerYear":"2","interestRate":"10"}
    @RequestMapping(value = "/amortization/schedule.json", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public List<ScheduledPayment> getAmortizationSchedule(@RequestBody AmortizationAttributes amAttrs) {
        return amortizationService.getAmortizationSchedule(amAttrs);
    }


    @RequestMapping(value = "/amortization/monthlyPayment", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public Money getMonthlyPayment(@RequestBody AmortizationAttributes amAttrs) {
        return amortizationService.getMonthlyPayment(amAttrs);
        //return new SimpleEntry<>("monthlyPayment", amortizationService.getMonthlyPayment(amAttrs) );
    }


}
