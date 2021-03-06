/* Copyright 2016-2017 Esteve Fernandez <esteve@apache.org>
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

package org.ros2.rcljava.node;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.ros2.rcljava.internal.message.Message;
import org.ros2.rcljava.internal.service.MessageService;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.service.Client;
import org.ros2.rcljava.node.service.NativeClient;
import org.ros2.rcljava.node.service.NativeService;
import org.ros2.rcljava.node.service.NativeServiceType;
import org.ros2.rcljava.node.service.NativeServiceType.ServiceDirection;
import org.ros2.rcljava.node.service.Service;
import org.ros2.rcljava.node.service.ServiceCallback;
import org.ros2.rcljava.node.topic.NativePublisher;
import org.ros2.rcljava.node.topic.NativeSubscription;
import org.ros2.rcljava.node.topic.Publisher;
import org.ros2.rcljava.node.topic.Subscription;
import org.ros2.rcljava.node.topic.SubscriptionCallback;
import org.ros2.rcljava.qos.QoSProfile;
import org.ros2.rcljava.time.NativeWallTimer;
import org.ros2.rcljava.time.WallTimer;
import org.ros2.rcljava.time.WallTimerCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves as a bridge between ROS2's rcl_node_t and RCLJava.
 * A Node must be created via @{link RCLJava#createNode(String)}
 */
public class NativeNode extends BaseNode {

    private static final Logger logger = LoggerFactory.getLogger(NativeNode.class);

    /**
     * An integer that represents a pointer to the underlying ROS2 node
     * structure (rcl_node_t).
     */
    private final long nodeHandle;

    // Native call.

    /**
     * Create a ROS2 node (rcl_node_t) and return a pointer to it as an integer.
     *
     * @param nodeName The name that will identify this node in a ROS2 graph.
     * @param spaceName The name-space that will identify this node in a ROS2 graph.
     * @param args
     * @return A pointer to the underlying ROS2 node structure.
     */
    private static native long nativeCreateNodeHandle(String nodeName, String spaceName, String... varargs);

    private static native void nativeDispose(long nodeHandle);

    private static native String nativeGetName(long nodeHandle);

    private static native int nativeCountPublishers(long nodeHandle, String topic);

    private static native int nativeCountSubscribers(long nodeHandle, String topic);

    private static native Map<String, List<String>> nativeGetListTopics(long nodeHandle, boolean noDemangle);

    private static native Map<String, List<String>> nativeGetListServices(long nodeHandle);

    private static native List<String> nativeGetNodeNames(long nodeHandle);

//    private static native  ; //rcl_service_server_is_available


    /**
     * Constructor.
     *
     * @param defaultName name of node.
     */
    public NativeNode(final String defaultName) {
        this(null, defaultName);
    }

    /**
     * Constructor.
     *
     * @param namespace prefix path of node.
     * @param defaultName name of node.
     * @param args options
     */
    public NativeNode(final String namespace, final String defaultName, final String... args) {
        super(namespace, defaultName, args);

        // Initialize native component.
        this.nodeHandle = NativeNode.nativeCreateNodeHandle(this.name, this.nameSpace, args);
        if (this.nodeHandle==0) { throw new NullPointerException("Node Handle is not define !"); }

        NativeNode.logger.debug(
                String.format("Created Native Node : %s [0x%x]",
                        GraphName.getFullName(this.nameSpace, this.name),
                        this.nodeHandle));

        this.startParameterService();
    }

    /**
     * Release all resource.
     */
    @Override
    public void dispose() {
        super.dispose();

        NativeNode.logger.debug(
                String.format("Destroy Native Node : %s [0x%x]",
                        GraphName.getFullName(this.nameSpace, this.name),
                        this.nodeHandle));
        NativeNode.nativeDispose(this.nodeHandle);
    }

    /**
     * Get the name of the node.
     *
     * @return The name of the node.
     */
    @Override
    public String getName() {
        String name = NativeNode.nativeGetName(this.nodeHandle);

        if (name != null && name.contains("/")) {
            if (this.nameSpace != null) {
                name = name.replace(this.nameSpace+"/", "");
            } else {
                name = name.replace("/", "");
            }
        }

        if (!this.name.equals(name)) {
            NativeNode.logger.debug("Node name has changed ! from " + this.name + " to " + name);
            this.name = name;
        }

        return name;
    }

    /**
     * Create and return a Publisher.
     *
     * @param <T> Message definition.
     * @param messageType Message class.
     * @param topicName The topic for this publisher to publish on.
     * @param qosProfile The quality of service profile to pass on to the rmw implementation.
     * @return Publisher instance of the created publisher.
     */
    @Override
    public <T extends Message> Publisher<T> createPublisher(
            final Class<T> messageType,
            final String topicName,
            final QoSProfile qosProfile) {

        if (messageType == null) { throw new NullPointerException(ERROR_MSG); }
        if (topicName   == null) { throw new NullPointerException(ERROR_TOPIC_MSG); }
        if (qosProfile  == null) { throw new NullPointerException(ERROR_QOS); }

        final String fqnTopic =  GraphName.getFullName(this, topicName, null);
        NativeNode.logger.debug("Initialize Native Publisher : " + fqnTopic);

        return new NativePublisher<T>(this, messageType, topicName, qosProfile);
    }

    /**
     * Create and return a Subscription.
     *
     * @param <T> Message definition.
     * @param messageType Message Class
     * @param topicName The topic to subscribe on.
     * @param callback The user-defined callback function.
     * @param qosProfile The quality of service profile to pass on to the rmw implementation.
     * @param ignoreLocalPublication True to ignore local publications.
     * @return Subscription instance of the created subscription.
     */
    @Override
    public <T extends Message> Subscription<T> createSubscription(
            final Class<T> messageType,
            final String topicName,
            final SubscriptionCallback<T> callback,
            final QoSProfile qosProfile,
            final boolean ignoreLocalPublication ) {  //TODO use it

        if (messageType == null) { throw new NullPointerException(ERROR_MSG); }
        if (topicName   == null) { throw new NullPointerException(ERROR_TOPIC_MSG); }
        if (callback    == null) { throw new NullPointerException(ERROR_CALLBACK); }
        if (qosProfile  == null) { throw new NullPointerException(ERROR_QOS); }

        final String fqnTopic =  GraphName.getFullName(this, topicName, null);
        NativeNode.logger.debug("Initialize Native Subscription : " + fqnTopic);

        return new NativeSubscription<T>(
                    this,
                    messageType,
                    topicName,
                    callback,
                    qosProfile);
    }

    /**
     * Create a timer.
     *
     * @param period The time interval between triggers of the callback.
     * @param unit 	The unit of time interval.
     * @param callback The user-defined callback function.
     * @return WallTimer instance of the created timer.
     */
    @Override
    public WallTimer createWallTimer(
            final long period,
            final TimeUnit unit,
            final WallTimerCallback callback) {

        final long timerPeriodNS = TimeUnit.NANOSECONDS.convert(period, unit);

        NativeNode.logger.debug("Initialize Native WallTimer.");
        return new NativeWallTimer(new WeakReference<Node>(this), callback, timerPeriodNS);
    }

    /**
     * Create and return a Client.
     *
     * @param <T> Message definition.
     * @param serviceType Service Class
     * @param serviceName The service to subscribe on.
     * @param qosProfile The quality of service profile to pass on to the rmw implementation.
     * @return Client instance of the service.
     */
    @Override
    public <T extends MessageService> Client<T> createClient(
            final Class<T> serviceType,
            final String serviceName,
            final QoSProfile qosProfile) {

        if (serviceType == null) { throw new NullPointerException(ERROR_SRV); }
        if (serviceName == null) { throw new NullPointerException(ERROR_TOPIC_SRV); }
        if (qosProfile  == null) { throw new NullPointerException(ERROR_QOS); }

        final String fqnService =  GraphName.getFullName(this, serviceName, null);
        NativeNode.logger.debug("Initialize Native Client : " + fqnService);

        final NativeServiceType<T> request = new NativeServiceType<T>(serviceType, ServiceDirection.REQUEST);
        final NativeServiceType<T> response = new NativeServiceType<T>(serviceType, ServiceDirection.RESPONSE);

        return new NativeClient<T>(
                this,
                serviceType,
                serviceName,
                request,
                response,
                qosProfile);
    }

    /**
     * Create and return a Service.
     *
     * @param <T> Message definition.
     * @param serviceType Service Class
     * @param serviceName The service for this publisher to publish on.
     * @param callback The user-defined callback function.
     * @param qosProfile The quality of service profile to pass on to the rmw implementation.
     * @return Service instance of the service.
     */
    @Override
    public <T extends MessageService> Service<T> createService(
            final Class<T> serviceType,
            final String serviceName,
            final ServiceCallback<?, ?> callback,
            final QoSProfile qosProfile
            ) {

        if (serviceType == null) { throw new NullPointerException(ERROR_SRV); }
        if (serviceName == null) { throw new NullPointerException(ERROR_TOPIC_SRV); }
        if (callback    == null) { throw new NullPointerException(ERROR_CALLBACK); }
        if (qosProfile  == null) { throw new NullPointerException(ERROR_QOS); }

        final String fqnService =  GraphName.getFullName(this, serviceName, null);
        NativeNode.logger.debug("Initialize Native Service : " + fqnService);

        final NativeServiceType<T> request = new NativeServiceType<T>(serviceType, ServiceDirection.REQUEST);
        final NativeServiceType<T> response = new NativeServiceType<T>(serviceType, ServiceDirection.RESPONSE);

        return new NativeService<T>(
                    this,
                    serviceType,
                    serviceName,
                    callback,
                    request,
                    response,
                    qosProfile);
    }

    @Override
    public Map<String, List<String>> getTopicNamesAndTypes(final boolean noDemangle) {
        final Map<String, List<String>> topics =  NativeNode.nativeGetListTopics(this.nodeHandle, noDemangle);

        for (final Entry<String, List<String>> entry : topics.entrySet()) {
            NativeNode.logger.debug("\t - Topics: " + entry.getKey() + "\t Value: " + entry.getValue());
        }

        return topics;
    }

    @Override
    public Map<String, List<String>> getServiceNamesAndTypes() {
        final Map<String, List<String>> services =  NativeNode.nativeGetListServices(this.nodeHandle);

        for (final Entry<String, List<String>> entry : services.entrySet()) {
            NativeNode.logger.debug("\t - Service: " + entry.getKey() + "\t Value: " + entry.getValue());
        }

        return services;
    }

    public Queue<NativeSubscription<? extends Message>> getNativeSubscriptions() {
        final Queue<NativeSubscription<? extends Message>> result = new LinkedBlockingDeque<NativeSubscription<? extends Message>>();

        for (final Subscription<? extends Message> subscription : this.getSubscriptions()) {
            result.add((NativeSubscription<? extends Message>)subscription);
        }

        return result;
    }

    public Queue<NativePublisher<? extends Message>> getNativePublishers() {
        final Queue<NativePublisher<? extends Message>> result = new LinkedBlockingDeque<NativePublisher<? extends Message>>();

        for (final Publisher<? extends Message> publisher : this.getPublishers()) {
            result.add((NativePublisher<? extends Message>)publisher);
        }

        return result;
    }

    public Queue<NativeClient<? extends MessageService>> getNativeClients() {
        final Queue<NativeClient<? extends MessageService>> result = new LinkedBlockingDeque<NativeClient<? extends MessageService>>();

        for (final Client<? extends MessageService> client : this.getClients()) {
            result.add((NativeClient<? extends MessageService>)client);
        }

        return result;
    }

    public Queue<NativeService<? extends MessageService>> getNativeServices() {
        final Queue<NativeService<? extends MessageService>> result = new LinkedBlockingDeque<NativeService<? extends MessageService>>();

        for (final Service<? extends MessageService> service : this.getServices()) {
            result.add((NativeService<? extends MessageService>)service);
        }

        return result;
    }

    public Queue<NativeWallTimer> getNativeWallTimers() {
        final Queue<NativeWallTimer> result = new LinkedBlockingDeque<NativeWallTimer>();

        for (final WallTimer wallTimer : this.getWallTimers()) {
            result.add((NativeWallTimer)wallTimer);
        }
        return result;
    }

    @Override
    public List<String> getNodeNames() {
        NativeNode.logger.debug("Get Native Node Names...");
        return NativeNode.nativeGetNodeNames(this.nodeHandle);
    }

    @Override
    public int countPublishers(final String topic) {
        NativeNode.logger.debug("Count Native Publisher...");
        return NativeNode.nativeCountPublishers(this.nodeHandle, topic);
    }

    @Override
    public int countSubscribers(final String topic) {
        NativeNode.logger.debug("Count Native Subscribers...");
        return NativeNode.nativeCountSubscribers(this.nodeHandle, topic);
    }

    /**
     * TODO REMOVE !!!
     * Return the rcl_node_t node handle (non-const version).
     * @return Node handler.
     */
    public long getNodeHandle() {
        return this.nodeHandle;
    }

}
