/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.box;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxAPIResponseException;
import com.box.sdk.BoxCollaboration;
import com.box.sdk.BoxCollaborator;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxGroup;
import com.box.sdk.BoxResourceIterable;
import com.box.sdk.BoxUser;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBoxFileCollaboratorsTest extends AbstractBoxFileTest {
    private static final String TEST_USER_ID_1 = "user1";
    private static final String TEST_USER_ID_2 = "user2";
    private static final String TEST_USER_ID_3 = "user3";
    private static final String TEST_GROUP_ID_1 = "group1";
    private static final String TEST_GROUP_ID_2 = "group2";
    private static final String TEST_USER_EMAIL_1 = "user1@example.com";
    private static final String TEST_USER_EMAIL_2 = "user2@example.com";
    private static final String TEST_USER_EMAIL_3 = "user3@example.com";
    private static final String TEST_GROUP_EMAIL_1 = "group1@example.com";
    private static final String TEST_GROUP_EMAIL_2 = "group2@example.com";

    @Mock
    BoxFile mockBoxFile;

    @Mock
    BoxCollaboration.Info mockCollabInfo1;

    @Mock
    BoxCollaboration.Info mockCollabInfo2;

    @Mock
    BoxCollaboration.Info mockCollabInfo3;

    @Mock
    BoxCollaboration.Info mockCollabInfo4;

    @Mock
    BoxCollaboration.Info mockCollabInfo5;

    @Mock
    BoxUser.Info mockUserInfo1;

    @Mock
    BoxUser.Info mockUserInfo2;

    @Mock
    BoxUser.Info mockUserInfo3;

    @Mock
    BoxGroup.Info mockGroupInfo1;

    @Mock
    BoxGroup.Info mockGroupInfo2;

    @Mock
    BoxResourceIterable<BoxCollaboration.Info> mockCollabIterable;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        final GetBoxFileCollaborators testSubject = new GetBoxFileCollaborators() {
            @Override
            protected BoxFile getBoxFile(String fileId) {
                return mockBoxFile;
            }
        };

        testRunner = TestRunners.newTestRunner(testSubject);
        super.setUp();
    }

    @Test
    void testGetCollaborationsFromFlowFileAttribute() {
        testRunner.setProperty(GetBoxFileCollaborators.FILE_ID, "${box.id}");
        final MockFlowFile inputFlowFile = new MockFlowFile(0);
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(BoxFileAttributes.ID, TEST_FILE_ID);
        inputFlowFile.putAttributes(attributes);

        setupMockCollaborations();

        testRunner.enqueue(inputFlowFile);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(GetBoxFileCollaborators.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(GetBoxFileCollaborators.REL_SUCCESS);
        final MockFlowFile flowFilesFirst = flowFiles.getFirst();

        flowFilesFirst.assertAttributeEquals("box.collaborations.count", "5");
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.users.ids", TEST_USER_ID_1 + "," + TEST_USER_ID_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.groups.ids", TEST_GROUP_ID_1);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.users.ids", TEST_USER_ID_3);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.groups.ids", TEST_GROUP_ID_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.users.emails", TEST_USER_EMAIL_1 + "," + TEST_USER_EMAIL_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.groups.emails", TEST_GROUP_EMAIL_1);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.users.emails", TEST_USER_EMAIL_3);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.groups.emails", TEST_GROUP_EMAIL_2);
    }

    @Test
    void testGetCollaborationsFromProperty() {
        testRunner.setProperty(GetBoxFileCollaborators.FILE_ID, TEST_FILE_ID);

        setupMockCollaborations();

        final MockFlowFile inputFlowFile = new MockFlowFile(0);
        testRunner.enqueue(inputFlowFile);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(GetBoxFileCollaborators.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(GetBoxFileCollaborators.REL_SUCCESS);
        final MockFlowFile flowFilesFirst = flowFiles.getFirst();

        flowFilesFirst.assertAttributeEquals("box.collaborations.count", "5");
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.users.ids", TEST_USER_ID_1 + "," + TEST_USER_ID_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.groups.ids", TEST_GROUP_ID_1);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.users.ids", TEST_USER_ID_3);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.groups.ids", TEST_GROUP_ID_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.users.emails", TEST_USER_EMAIL_1 + "," + TEST_USER_EMAIL_2);
        flowFilesFirst.assertAttributeEquals("box.collaborations.accepted.groups.emails", TEST_GROUP_EMAIL_1);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.users.emails", TEST_USER_EMAIL_3);
        flowFilesFirst.assertAttributeEquals("box.collaborations.pending.groups.emails", TEST_GROUP_EMAIL_2);
    }

    @Test
    void testApiErrorHandling() {
        testRunner.setProperty(GetBoxFileCollaborators.FILE_ID, TEST_FILE_ID);

        final BoxAPIResponseException mockException = new BoxAPIResponseException("API Error", 404, "Box File Not Found", null);
        when(mockBoxFile.getAllFileCollaborations()).thenThrow(mockException);

        final MockFlowFile inputFlowFile = new MockFlowFile(0);
        testRunner.enqueue(inputFlowFile);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(GetBoxFileCollaborators.REL_NOT_FOUND, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(GetBoxFileCollaborators.REL_NOT_FOUND);
        final MockFlowFile flowFilesFirst = flowFiles.getFirst();
        flowFilesFirst.assertAttributeEquals(BoxFileAttributes.ERROR_CODE, "404");
        flowFilesFirst.assertAttributeEquals(BoxFileAttributes.ERROR_MESSAGE, "API Error [404]");
    }

    @Test
    void testBoxApiExceptionHandling() {
        testRunner.setProperty(GetBoxFileCollaborators.FILE_ID, TEST_FILE_ID);

        final BoxAPIException mockException = new BoxAPIException("General API Error:", 500, "Unexpected Error");
        when(mockBoxFile.getAllFileCollaborations()).thenThrow(mockException);

        final MockFlowFile inputFlowFile = new MockFlowFile(0);
        testRunner.enqueue(inputFlowFile);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(GetBoxFileCollaborators.REL_FAILURE, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(GetBoxFileCollaborators.REL_FAILURE);
        final MockFlowFile flowFilesFirst = flowFiles.getFirst();

        flowFilesFirst.assertAttributeEquals(BoxFileAttributes.ERROR_CODE, "500");
        flowFilesFirst.assertAttributeEquals(BoxFileAttributes.ERROR_MESSAGE, "General API Error:\nUnexpected Error");
    }

    private void setupMockCollaborations() {
        setupCollaborator(mockCollabInfo1, mockUserInfo1, BoxCollaborator.CollaboratorType.USER, TEST_USER_ID_1, BoxCollaboration.Status.ACCEPTED);
        setupCollaborator(mockCollabInfo2, mockUserInfo2, BoxCollaborator.CollaboratorType.USER, TEST_USER_ID_2, BoxCollaboration.Status.ACCEPTED);
        setupCollaborator(mockCollabInfo3, mockGroupInfo1, BoxCollaborator.CollaboratorType.GROUP, TEST_GROUP_ID_1, BoxCollaboration.Status.ACCEPTED);
        setupCollaborator(mockCollabInfo4, mockUserInfo3, BoxCollaborator.CollaboratorType.USER, TEST_USER_ID_3, BoxCollaboration.Status.PENDING);
        setupCollaborator(mockCollabInfo5, mockGroupInfo2, BoxCollaborator.CollaboratorType.GROUP, TEST_GROUP_ID_2, BoxCollaboration.Status.PENDING);

        when(mockCollabIterable.iterator()).thenReturn(
                List.of(mockCollabInfo1, mockCollabInfo2, mockCollabInfo3, mockCollabInfo4, mockCollabInfo5).iterator()
        );

        when(mockBoxFile.getAllFileCollaborations()).thenReturn(mockCollabIterable);
    }

    private void setupCollaborator(final BoxCollaboration.Info collabInfo,
                                   final BoxCollaborator.Info collaboratorInfo,
                                   final BoxCollaborator.CollaboratorType type,
                                   final String id,
                                   final BoxCollaboration.Status status) {
        when(collabInfo.getAccessibleBy()).thenReturn(collaboratorInfo);
        when(collaboratorInfo.getType()).thenReturn(type);
        when(collaboratorInfo.getID()).thenReturn(id);
        when(collabInfo.getStatus()).thenReturn(status);

        final Map<String, String> userEmails = Map.of(
                TEST_USER_ID_1, TEST_USER_EMAIL_1,
                TEST_USER_ID_2, TEST_USER_EMAIL_2,
                TEST_USER_ID_3, TEST_USER_EMAIL_3
        );

        Map<String, String> groupEmails = Map.of(
                TEST_GROUP_ID_1, TEST_GROUP_EMAIL_1,
                TEST_GROUP_ID_2, TEST_GROUP_EMAIL_2
        );
        String email = null;
        if (type.equals(BoxCollaborator.CollaboratorType.USER)) {
            email = userEmails.getOrDefault(id, null);
        } else if (type.equals(BoxCollaborator.CollaboratorType.GROUP)) {
            email = groupEmails.getOrDefault(id, null);
        }

        when(collaboratorInfo.getLogin()).thenReturn(email);
    }
}