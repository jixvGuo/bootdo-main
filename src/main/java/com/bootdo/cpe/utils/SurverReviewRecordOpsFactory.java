package com.bootdo.cpe.utils;

import com.bootdo.cpe.domain.SurverReviewConsultResultDO;
import com.bootdo.cpe.domain.SurverReviewDesignResultDO;
import com.bootdo.cpe.domain.SurverReviewSoftResultDO;
import com.bootdo.cpe.domain.SurverReviewStandardResultDO;
import com.bootdo.cpe.domain.SurverReviewSurverResultDO;
import com.bootdo.cpe.service.SurverReviewConsultResultService;
import com.bootdo.cpe.service.SurverReviewDesignResultService;
import com.bootdo.cpe.service.SurverReviewSoftResultService;
import com.bootdo.cpe.service.SurverReviewStandardResultService;
import com.bootdo.cpe.service.SurverReviewSurverResultService;

/**
 * 各子奖项形审 {@link SurverReviewSaveHelper.RecordOps} 适配。
 */
public final class SurverReviewRecordOpsFactory {

    private SurverReviewRecordOpsFactory() {
    }

    public static SurverReviewSaveHelper.RecordOps<SurverReviewDesignResultDO> design(
            SurverReviewDesignResultService service) {
        return new SurverReviewSaveHelper.RecordOps<SurverReviewDesignResultDO>() {
            @Override
            public Integer getId(SurverReviewDesignResultDO entity) {
                return entity.getId();
            }

            @Override
            public void setId(SurverReviewDesignResultDO entity, Integer id) {
                entity.setId(id);
            }

            @Override
            public void setOptUid(SurverReviewDesignResultDO entity, int uid) {
                entity.setOptUid(uid);
            }

            @Override
            public String getReviewResult(SurverReviewDesignResultDO entity) {
                return entity.getReviewResult();
            }

            @Override
            public void setReviewResult(SurverReviewDesignResultDO entity, String value) {
                entity.setReviewResult(value);
            }

            @Override
            public String getRemarks(SurverReviewDesignResultDO entity) {
                return entity.getRemarks();
            }

            @Override
            public void setRemarks(SurverReviewDesignResultDO entity, String value) {
                entity.setRemarks(value);
            }

            @Override
            public String getProName(SurverReviewDesignResultDO entity) {
                return entity.getProName();
            }

            @Override
            public SurverReviewDesignResultDO load(Integer id) {
                return service.get(id);
            }

            @Override
            public int insert(SurverReviewDesignResultDO entity) {
                return service.save(entity);
            }

            @Override
            public int update(SurverReviewDesignResultDO entity) {
                return service.update(entity);
            }
        };
    }

    public static SurverReviewSaveHelper.RecordOps<SurverReviewSoftResultDO> soft(
            SurverReviewSoftResultService service) {
        return new SurverReviewSaveHelper.RecordOps<SurverReviewSoftResultDO>() {
            @Override
            public Integer getId(SurverReviewSoftResultDO entity) {
                return entity.getId();
            }

            @Override
            public void setId(SurverReviewSoftResultDO entity, Integer id) {
                entity.setId(id);
            }

            @Override
            public void setOptUid(SurverReviewSoftResultDO entity, int uid) {
                entity.setOptUid(uid);
            }

            @Override
            public String getReviewResult(SurverReviewSoftResultDO entity) {
                return entity.getReviewResult();
            }

            @Override
            public void setReviewResult(SurverReviewSoftResultDO entity, String value) {
                entity.setReviewResult(value);
            }

            @Override
            public String getRemarks(SurverReviewSoftResultDO entity) {
                return entity.getRemarks();
            }

            @Override
            public void setRemarks(SurverReviewSoftResultDO entity, String value) {
                entity.setRemarks(value);
            }

            @Override
            public String getProName(SurverReviewSoftResultDO entity) {
                return entity.getProName();
            }

            @Override
            public SurverReviewSoftResultDO load(Integer id) {
                return service.get(id);
            }

            @Override
            public int insert(SurverReviewSoftResultDO entity) {
                return service.save(entity);
            }

            @Override
            public int update(SurverReviewSoftResultDO entity) {
                return service.update(entity);
            }
        };
    }

    public static SurverReviewSaveHelper.RecordOps<SurverReviewStandardResultDO> standard(
            SurverReviewStandardResultService service) {
        return new SurverReviewSaveHelper.RecordOps<SurverReviewStandardResultDO>() {
            @Override
            public Integer getId(SurverReviewStandardResultDO entity) {
                return entity.getId();
            }

            @Override
            public void setId(SurverReviewStandardResultDO entity, Integer id) {
                entity.setId(id);
            }

            @Override
            public void setOptUid(SurverReviewStandardResultDO entity, int uid) {
                entity.setOptUid(uid);
            }

            @Override
            public String getReviewResult(SurverReviewStandardResultDO entity) {
                return entity.getReviewResult();
            }

            @Override
            public void setReviewResult(SurverReviewStandardResultDO entity, String value) {
                entity.setReviewResult(value);
            }

            @Override
            public String getRemarks(SurverReviewStandardResultDO entity) {
                return entity.getRemarks();
            }

            @Override
            public void setRemarks(SurverReviewStandardResultDO entity, String value) {
                entity.setRemarks(value);
            }

            @Override
            public String getProName(SurverReviewStandardResultDO entity) {
                return entity.getProName();
            }

            @Override
            public SurverReviewStandardResultDO load(Integer id) {
                return service.get(id);
            }

            @Override
            public int insert(SurverReviewStandardResultDO entity) {
                return service.save(entity);
            }

            @Override
            public int update(SurverReviewStandardResultDO entity) {
                return service.update(entity);
            }
        };
    }

    public static SurverReviewSaveHelper.RecordOps<SurverReviewConsultResultDO> consult(
            SurverReviewConsultResultService service) {
        return new SurverReviewSaveHelper.RecordOps<SurverReviewConsultResultDO>() {
            @Override
            public Integer getId(SurverReviewConsultResultDO entity) {
                return entity.getId();
            }

            @Override
            public void setId(SurverReviewConsultResultDO entity, Integer id) {
                entity.setId(id);
            }

            @Override
            public void setOptUid(SurverReviewConsultResultDO entity, int uid) {
                entity.setOptUid(uid);
            }

            @Override
            public String getReviewResult(SurverReviewConsultResultDO entity) {
                return entity.getReviewResult();
            }

            @Override
            public void setReviewResult(SurverReviewConsultResultDO entity, String value) {
                entity.setReviewResult(value);
            }

            @Override
            public String getRemarks(SurverReviewConsultResultDO entity) {
                return entity.getRemarks();
            }

            @Override
            public void setRemarks(SurverReviewConsultResultDO entity, String value) {
                entity.setRemarks(value);
            }

            @Override
            public String getProName(SurverReviewConsultResultDO entity) {
                return entity.getProName();
            }

            @Override
            public SurverReviewConsultResultDO load(Integer id) {
                return service.get(id);
            }

            @Override
            public int insert(SurverReviewConsultResultDO entity) {
                return service.save(entity);
            }

            @Override
            public int update(SurverReviewConsultResultDO entity) {
                return service.update(entity);
            }
        };
    }

    public static SurverReviewSaveHelper.RecordOps<SurverReviewSurverResultDO> surver(
            SurverReviewSurverResultService service) {
        return new SurverReviewSaveHelper.RecordOps<SurverReviewSurverResultDO>() {
            @Override
            public Integer getId(SurverReviewSurverResultDO entity) {
                return entity.getId();
            }

            @Override
            public void setId(SurverReviewSurverResultDO entity, Integer id) {
                entity.setId(id);
            }

            @Override
            public void setOptUid(SurverReviewSurverResultDO entity, int uid) {
                entity.setOptUid(uid);
            }

            @Override
            public String getReviewResult(SurverReviewSurverResultDO entity) {
                return entity.getReviewResult();
            }

            @Override
            public void setReviewResult(SurverReviewSurverResultDO entity, String value) {
                entity.setReviewResult(value);
            }

            @Override
            public String getRemarks(SurverReviewSurverResultDO entity) {
                return entity.getRemarks();
            }

            @Override
            public void setRemarks(SurverReviewSurverResultDO entity, String value) {
                entity.setRemarks(value);
            }

            @Override
            public String getProName(SurverReviewSurverResultDO entity) {
                return entity.getProName();
            }

            @Override
            public SurverReviewSurverResultDO load(Integer id) {
                return service.get(id);
            }

            @Override
            public int insert(SurverReviewSurverResultDO entity) {
                return service.save(entity);
            }

            @Override
            public int update(SurverReviewSurverResultDO entity) {
                return service.update(entity);
            }
        };
    }
}
