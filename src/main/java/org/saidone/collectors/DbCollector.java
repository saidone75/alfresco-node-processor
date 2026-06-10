/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2026 Saidone
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.model.config.CollectorConfig;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Collects node identifiers by querying the Alfresco database directly.
 * <p>
 * This collector uses a recursive SQL query to traverse the folder hierarchy
 * starting from a specified root node and collects all content nodes within
 * that tree. The collected node UUIDs are enqueued for processing.
 * <p>
 * Required configuration arguments:
 * <ul>
 *   <li>{@code root-node-id} - UUID of the root folder node</li>
 *   <li>{@code db-url} - JDBC connection URL to the Alfresco database</li>
 *   <li>{@code db-user} - Database username</li>
 *   <li>{@code db-password} - Database password</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbCollector extends AbstractNodeCollector {

    private static final String SQL_QUERY = """
        WITH RECURSIVE folder_tree AS (
            SELECT id
            FROM alf_node 
            WHERE uuid = ?
            UNION ALL
            SELECT n.id
            FROM alf_node n
            JOIN alf_child_assoc c ON c.child_node_id = n.id
            JOIN folder_tree ac ON c.parent_node_id = ac.id
            JOIN alf_qname q ON n.type_qname_id = q.id
            WHERE q.local_name IN ('folder', 'site', 'sitelist', 'folder-templates')
        )
        SELECT n_doc.uuid AS uuid
        FROM folder_tree ac
        JOIN alf_child_assoc c_doc ON c_doc.parent_node_id = ac.id
        JOIN alf_node n_doc ON c_doc.child_node_id = n_doc.id
        JOIN alf_qname q_doc ON n_doc.type_qname_id = q_doc.id
        WHERE q_doc.local_name = 'content'
        """;

    @Override
    public void collectNodes(CollectorConfig config) {
        extractUuidsByFolder(
                (String) config.getArg("root-node-id"),
                (String) config.getArg("db-url"),
                (String) config.getArg("db-user"),
                (String) config.getArg("db-password")
        );
    }

    public void extractUuidsByFolder(String rootUuid, String dbUrl, String user, String password) {
        long counter = 0;
        try (val conn = DriverManager.getConnection(dbUrl, user, password);
             val pstmt = conn.prepareStatement(SQL_QUERY)) {
            conn.setAutoCommit(false);
            pstmt.setFetchSize(1000);
            pstmt.setString(1, rootUuid);
            try (val rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    queue.put(rs.getString("uuid"));
                    counter++;
                }
            }
            conn.commit();
            log.info("Total UUIDs extracted from DB and queued: {}", counter);
        } catch (SQLException | InterruptedException e) {
            log.error("Error during extraction: {}", e.getMessage(), e);
        }
    }

}