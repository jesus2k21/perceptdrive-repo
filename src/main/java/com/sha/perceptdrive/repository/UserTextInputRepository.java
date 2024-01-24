package com.sha.perceptdrive.repository;

import com.sha.perceptdrive.entities.UserTextInput;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserTextInputRepository extends CrudRepository<UserTextInput, Integer> {
    //List<UserTextInput> getUserTextInputBytext_type(final String text_type);
}
