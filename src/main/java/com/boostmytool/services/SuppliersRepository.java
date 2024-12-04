package com.boostmytool.services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boostmytool.model.Supplier;

public interface SuppliersRepository extends JpaRepository<Supplier, String>{

	
}
