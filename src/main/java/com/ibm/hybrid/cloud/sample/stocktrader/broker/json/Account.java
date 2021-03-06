/*
       Copyright 2020 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.broker.json;


/** JSON-B POJO class representing an Account JSON object */
public class Account {
    private String _id;
    private String _rev;
    private String owner;
    private String loyalty;
    private double balance;
    private double commissions;
    private int free;
    private String sentiment;
    private double nextCommission;

    public Account() { //default constructor
    }

    public Account(String initialOwner) { //primary key constructor
        setOwner(initialOwner);
    }

    public Account(String initialOwner, String initialLoyalty, double initialBalance, double initialCommissions,
                     int initialFree, String initialSentiment, double initialNextCommission) {
        setOwner(initialOwner);
        setLoyalty(initialLoyalty);
        setBalance(initialBalance);
        setCommissions(initialCommissions);
        setFree(initialFree);
        setSentiment(initialSentiment);
        setNextCommission(initialNextCommission);
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String new_id) {
        _id = new_id;
    }

    public String get_rev() {
        return _rev;
    }

    public void set_rev(String new_rev) {
        _rev = new_rev;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String newOwner) {
        owner = newOwner;
    }

    public String getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(String newLoyalty) {
        loyalty = newLoyalty;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double newBalance) {
        balance = newBalance;
    }

    public double getCommissions() {
        return commissions;
    }

    public void setCommissions(double newCommissions) {
        commissions = newCommissions;
    }

    public int getFree() {
        return free;
    }

    public void setFree(int newFree) {
        free = newFree;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String newSentiment) {
        sentiment = newSentiment;
    }

    public double getNextCommission() {
        return nextCommission;
    }

    public void setNextCommission(double newNextCommission) {
        nextCommission = newNextCommission;
    }

    public boolean equals(Object obj) {
        boolean isEqual = false;
        if ((obj != null) && (obj instanceof Account)) isEqual = toString().equals(obj.toString());
        return isEqual;
   }

    public String toString() {
        return "{\"_id\": \""+_id+"\", \"_rev\": \""+_rev+"\", \"owner\": \""+owner+"\", \"loyalty\": \""+loyalty
               +"\", \"balance\": "+balance+", \"commissions\": "+commissions+", \"free\": "+free
               +", \"nextCommission\": "+nextCommission+", \"sentiment\": \""+sentiment+"\"}";
    }
}
