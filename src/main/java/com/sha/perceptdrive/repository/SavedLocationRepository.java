package com.sha.perceptdrive.repository;

import com.sha.perceptdrive.entities.SavedLocation;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SavedLocationRepository extends CrudRepository<SavedLocation, Integer> {

     SavedLocation findSavedLocationByPhoneAndNicknameIgnoreCase(final String phone, final String nickname);
     List<SavedLocation> findAllByPhone(final String phone);
}
