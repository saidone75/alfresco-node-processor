package org.saidone.collectors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.search.handler.SearchApi;
import org.alfresco.search.model.*;
import org.saidone.model.alfresco.SystemSearchRequest;
import org.saidone.model.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class QueryNodeCollector extends AbstractNodeCollector{

    @Value("${application.search-batch-size}")
    private int batchSize;

    @Autowired
    private SearchApi searchApi;

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

    @Override
    public void collectNodes(Config config) {
        executor.submit(() -> doQuery(config.getQuery()));
    }

}
