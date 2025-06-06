/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023-2024 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.saidone.collectors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.search.handler.SearchApi;
import org.alfresco.search.model.*;
import org.saidone.model.config.CollectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class QueryNodeCollector extends AbstractNodeCollector {

    private int batchSize = 100;

    @Autowired
    private SearchApi searchApi;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SneakyThrows
    private ResultSetPaging search(String query, int skipCount) {
        var searchRequest = new SearchRequest();
        var requestQuery = new RequestQuery();
        requestQuery.setLanguage(RequestQuery.LanguageEnum.AFTS);
        requestQuery.setQuery(query);
        var paging = new RequestPagination();
        paging.setMaxItems(batchSize);
        paging.setSkipCount(skipCount);
        searchRequest.setQuery(requestQuery);
        searchRequest.setPaging(paging);
        return searchApi.search(searchRequest).getBody();
    }

    @SneakyThrows
    private Void doQuery(String query) {
        var skipCount = 0;
        ResultSetPaging resultSetPaging;
        do {
            log.debug("skipCount --> {}", skipCount);
            resultSetPaging = search(query, skipCount);
            for (var e : resultSetPaging.getList().getEntries()) {
                queue.put(e.getEntry().getId());
            }
            skipCount += batchSize;
        } while (resultSetPaging.getList().getPagination().isHasMoreItems());
        return null;
    }

    @Override
    public void collectNodes(CollectorConfig config) {
        if (config.getArg("batch-size") != null) this.batchSize = (int) config.getArg("batch-size");
        executor.submit(() -> doQuery((String) config.getArg("query")));
    }

}
