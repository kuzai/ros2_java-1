/* Copyright 2016 Esteve Fernandez <esteve@apache.org>
 * Copyright 2016-2018 Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ros2.rcljava.node.service;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.internal.service.MessageService;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.NativeNode;
import org.ros2.rcljava.qos.QoSProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is Native(rcl) Service Server of RCLJava.
 *
 * param <T> Service Type.
 */
public class NativeService<T extends MessageService> extends BaseService<T> {

    private static final Logger logger = LoggerFactory.getLogger(NativeService.class);

    /** Service Handler. */
    private final long serviceHandle;

    private final NativeServiceType<T> request;
    private final NativeServiceType<T> response;

    private static native <T> long nativeCreateServiceHandle(
            long nodeHandle, Class<T> cls, String serviceName, long qosProfileHandle);

    private static native void nativeDispose(long nodeHandle, long serviceHandle);

    /**
     * Constructor.
     *
     * @param nodeReference
     * @param serviceType
     * @param serviceName
     * @param callback
     * @param request
     * @param response
     * @param qosProfile
     */
    public NativeService(
            final NativeNode nodeReference,
            final Class<T> serviceType,
            final String serviceName,
            final ServiceCallback<?, ?> callback,
            final NativeServiceType<T> request,
            final NativeServiceType<T> response,
            final QoSProfile qosProfile) {
        super(nodeReference, serviceType, serviceName, callback, request.getType(), response.getType());

        final String fqnService =  GraphName.getFullName(nodeReference, serviceName, null);
        if (!GraphName.isValidTopic(fqnService)) { throw new RuntimeException("Invalid topic name."); }

        final long qosProfileHandle = RCLJava.convertQoSProfileToHandle(qosProfile);
        this.serviceHandle = NativeService.nativeCreateServiceHandle(
                nodeReference.getNodeHandle(),
                serviceType,
                fqnService,
                qosProfileHandle);
        RCLJava.disposeQoSProfile(qosProfileHandle);
        if (this.serviceHandle == 0) { throw new RuntimeException("Need to provide active service with handle object"); }

        this.request = request;
        this.response = response;

        NativeService.logger.debug(
                String.format("Created Native Service Server : %s  [0x%x] (request : [0x%x]> <[0x%x]) (response : [0x%x]> <[0x%x])",
                        this.getServiceName(),
                        this.serviceHandle,
                        this.request.getFromJavaConverterHandle(),
                        this.request.getToJavaConverterHandle(),
                        this.response.getFromJavaConverterHandle(),
                        this.response.getToJavaConverterHandle()));
    }

    @Override
    public void dispose() {
        super.dispose();

        NativeService.logger.debug(
                String.format("Destroy Native Service Client : %s  [0x%x] (request : [0x%x]> <[0x%x]) (response : [0x%x]> <[0x%x])",
                        this.getServiceName(),
                        this.serviceHandle,
                        this.request.getFromJavaConverterHandle(),
                        this.request.getToJavaConverterHandle(),
                        this.response.getFromJavaConverterHandle(),
                        this.response.getToJavaConverterHandle()));

      NativeService.nativeDispose(this.getNode().getNodeHandle(), this.serviceHandle);
    }

//    public void sendResponse() {
//
//    }

    @Override
    public NativeNode getNode() {
        return (NativeNode) super.getNode();
    }

    public long getServiceHandle() {
        return this.serviceHandle;
    }

    public NativeServiceType<T> getRequest() {
        return this.request;
    }

    public NativeServiceType<T> getResponse() {
        return this.response;
    }

}
