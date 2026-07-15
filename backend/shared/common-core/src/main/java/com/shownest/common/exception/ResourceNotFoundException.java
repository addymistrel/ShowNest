package com.shownest.common.exception;

import com.shownest.common.enums.ErrorCode;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceName + " not found with id: " + id);
    }
}
