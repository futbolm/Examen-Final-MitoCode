package com.examen.service.Impl;

import com.examen.model.EnrollmentDetail;
import com.examen.repo.IGenericRepo;
import com.examen.service.IEnrollmentDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IEnrollmentDetailServiceImpl extends CRUDImpl<EnrollmentDetail,Integer> implements IEnrollmentDetailService {

    private final IGenericRepo<EnrollmentDetail,Integer> repo;

    @Override
    protected IGenericRepo<EnrollmentDetail, Integer> getRepo() {
        return repo;
    }
}
