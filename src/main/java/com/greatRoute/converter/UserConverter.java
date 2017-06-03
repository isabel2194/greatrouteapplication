package com.greatRoute.converter;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.greatRoute.entity.Usuario;
import com.greatRoute.model.UserModel;

@Component("userConverter")
public class UserConverter {
	
	@Autowired
	ModelMapper modelMapper;

	// Entity --> Model
	public UserModel entityToModel(Usuario user) {
		return modelMapper.map(user, UserModel.class);
	}

	// Model --> Entity
	public Usuario modelToEntity(UserModel userModel) {
		return modelMapper.map(userModel, Usuario.class);
	}
}
