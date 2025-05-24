package com.bsuir.bankliquiditserver.model;

import java.io.Serializable;
import java.util.Objects;

public class Bank implements Serializable {
    private static final long serialVersionUID = 3L;

    private int id;
    private String name;
    private String registrationNumber;
    private String address;

    // Конструкторы, геттеры/сеттеры, equals/hashCode/toString
    public Bank() {
    }

    public Bank(int id, String name, String registrationNumber, String address) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bank bank = (Bank) o;
        return id == bank.id &&
                Objects.equals(name, bank.name) &&
                Objects.equals(registrationNumber, bank.registrationNumber) &&
                Objects.equals(address, bank.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, registrationNumber, address);
    }

    @Override
    public String toString() {
        return "Bank{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}