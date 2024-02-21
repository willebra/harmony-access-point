package eu.domibus.ext.rest;

import eu.domibus.ext.domain.ErrorDTO;
import eu.domibus.ext.exceptions.TrustedListExtException;
import eu.domibus.ext.rest.error.ExtExceptionHelper;
import eu.domibus.ext.services.TrustedListExtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * Triggers a refresh of the Trusted Lists in DSS
 *
 * @author Breaz Ionut
 * @since 5.0.8
 */


@RestController
@RequestMapping(value = "/ext/trustedlists")
@Tag(name = "trustedLists", description = "Trusted Lists API")
public class DssTrustedListsResource {
    protected TrustedListExtService trustedListExtService;
    protected final ExtExceptionHelper extExceptionHelper;

    public DssTrustedListsResource(TrustedListExtService trustedListExtService, ExtExceptionHelper extExceptionHelper) {
        this.trustedListExtService = trustedListExtService;
        this.extExceptionHelper = extExceptionHelper;
    }

    @ExceptionHandler(TrustedListExtException.class)
    public ResponseEntity<ErrorDTO> handlePartyExtServiceException(TrustedListExtException e) {
        return extExceptionHelper.handleExtException(e);
    }

    @Operation(summary = "Refresh trusted lists", description = "Triggers a new download of the DSS Trusted Lists and apply needed changes in DSS list of trusted CAs",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @PostMapping("/refreshoperation")
    public void refreshTrustedLists() {
        trustedListExtService.refreshTrustedLists();
    }
}
