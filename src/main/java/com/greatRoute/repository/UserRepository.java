package com.greatRoute.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greatRoute.entity.Usuario;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<Usuario, Serializable>{

	public abstract Usuario findByUsername(String username);
	public abstract Usuario findByUsernameAndPassword(String username, String password);
	public abstract Usuario findByUsernameAndEmail(String username, String email);
	
}
