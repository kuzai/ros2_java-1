/* Copyright 2018 Mickael Gaillard <mick.gaillard@gmail.com>
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

package org.ros2.rcljava.node.topic;

import org.ros2.rcljava.exception.NotImplementedException;
import org.ros2.rcljava.internal.message.Message;
import org.ros2.rcljava.node.JavaNode;
import org.ros2.rcljava.qos.QoSProfile;

/**
 * This class is JVM Publisher of RCLJava.
 * <b>Actually not implemented !!!</b>
 */
public class JavaPublisher<T extends Message> extends BasePublisher<T> {

    /**
     *
     * @param node
     * @param messageType
     * @param topic
     * @param qosProfile
     */
    public JavaPublisher(
            final JavaNode node,
            final Class<T> messageType,
            final String topic,
            final QoSProfile qosProfile) {
        super(node, messageType, topic, qosProfile);

        throw new NotImplementedException();
    }

    /* (non-Javadoc)
     * @see org.ros2.rcljava.node.topic.Publisher#getNode()
     */
    @Override
    public JavaNode getNode() {
        return (JavaNode) super.getNode();
    }

    /* (non-Javadoc)
     * @see org.ros2.rcljava.node.topic.Publisher#publish(org.ros2.rcljava.internal.message.Message)
     */
    @Override
    public void publish(final T message) {
        // TODO Auto-generated method stub

    }

}
