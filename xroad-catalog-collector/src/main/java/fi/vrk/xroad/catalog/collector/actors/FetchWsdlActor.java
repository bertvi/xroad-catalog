package fi.vrk.xroad.catalog.collector.actors;

import akka.actor.Terminated;
import akka.actor.UntypedActor;
import com.google.common.base.Strings;
import eu.x_road.xsd.xroad.ClientType;
import fi.vrk.xroad.catalog.collector.util.XRoadClient;
import fi.vrk.xroad.catalog.collector.wsimport.XRoadServiceIdentifierType;
import fi.vrk.xroad.catalog.persistence.entity.Member;
import fi.vrk.xroad.catalog.persistence.entity.Service;
import fi.vrk.xroad.catalog.persistence.entity.Subsystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Actor which fetches one wsdl
 */
@Component
@Scope("prototype")
@Slf4j
public class FetchWsdlActor extends UntypedActor {

    private static AtomicInteger COUNTER = new AtomicInteger(0);
    private static final String WSDL_CONTEXT_PATH = "/wsdl";

    @Value("${xroad-catalog.security-server-host:http://localhost}")
    private String host;

    // TODO: make configurable
    public String getHost() {
        return host;
    }


    @Autowired
    @Qualifier("wsdlRestOperations")
    private RestOperations restOperations;

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof XRoadServiceIdentifierType) {
            log.info("fetching wsdl [{}] {}", COUNTER.addAndGet(1), message);
            XRoadServiceIdentifierType service = (XRoadServiceIdentifierType) message;
            // get wsdl
            String url = buildUri(service);
            log.info("reading wsdl from url: "  + url);
            String wsdl = restOperations.getForObject(url, String.class);
            log.info("received wsdl: " + wsdl);

        } else if (message instanceof Terminated) {
            throw new RuntimeException("Terminated: " + message);
        } else {
            log.error("Unable to handle message {}", message);
        }
    }

    private String buildUri(XRoadServiceIdentifierType service) {
        assert service.getXRoadInstance() != null;
        assert service.getMemberClass() != null;
        assert service.getMemberCode() != null;
        assert service.getServiceCode() != null;
        assert service.getServiceVersion() != null;

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getHost())
                .path(WSDL_CONTEXT_PATH)
                .queryParam("xRoadInstance", service.getXRoadInstance())
                .queryParam("memberClass", service.getMemberClass())
                .queryParam("memberCode", service.getMemberCode())
                .queryParam("serviceCode", service.getServiceCode())
                .queryParam("version", service.getServiceVersion());
        if (!Strings.isNullOrEmpty(service.getSubsystemCode())) {
            builder = builder.queryParam("subsystemCode", service.getSubsystemCode());
        }
        return builder.toUriString();
    }
}
