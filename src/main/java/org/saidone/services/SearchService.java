/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023 Saidone
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

package org.saidone.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.search.handler.SearchApi;
import org.alfresco.search.model.*;
import org.saidone.model.alfresco.SystemSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class SearchService {

    @Autowired
    private SearchApi searchApi;

    @Autowired
    private LinkedBlockingQueue<String> queue;

    @Value("${application.search-batch-size}")
    private int batchSize;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SneakyThrows
    private ResultSetPaging search(SystemSearchRequest systemSearchRequest) {
        var searchRequest = new SearchRequest();
        var requestQuery = new RequestQuery();
        requestQuery.setLanguage(RequestQuery.LanguageEnum.AFTS);
        requestQuery.setQuery(systemSearchRequest.getQuery());
        var paging = new RequestPagination();
        paging.setMaxItems(systemSearchRequest.getMaxItems());
        paging.setSkipCount(systemSearchRequest.getSkipCount());
        searchRequest.setQuery(requestQuery);
        searchRequest.setPaging(paging);
        return searchApi.search(searchRequest).getBody();
    }

    @SneakyThrows
    public Void doQuery(String query) {
        var searchRequest = new SystemSearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setSkipCount(0);
        searchRequest.setMaxItems(batchSize);
        ResultSetPaging resultSetPaging;
        do {
            log.debug("skipCount --> {}", searchRequest.getSkipCount());
            resultSetPaging = search(searchRequest);
            for (ResultSetRowEntry e : resultSetPaging.getList().getEntries()) {
                queue.put(e.getEntry().getId());
            }
            searchRequest.setSkipCount(searchRequest.getSkipCount() + batchSize);
        } while (resultSetPaging.getList().getPagination().isHasMoreItems());
        return null;
    }

    public void submitQuery(String query) {
        executor.submit(() -> doQuery(query));
    }

}
