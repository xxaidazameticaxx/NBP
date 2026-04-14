package ba.unsa.etf.NBP.security;

import ba.unsa.etf.NBP.model.Role;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionFilterTest {

    @Mock
    private AuthService authService;

    private SessionFilter sessionFilter;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        sessionFilter = new SessionFilter(authService);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void protectedRequestWithoutHeaderReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/students");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        sessionFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(authService, never()).authenticateSession(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void protectedRequestWithInvalidHeaderReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/students");
        request.addHeader(AuthService.AUTHORIZATION_HEADER, "Bearer fake-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(authService.authenticateSession("Bearer fake-token")).thenReturn(Optional.empty());

        sessionFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void protectedRequestWithValidSessionSetsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/students");
        request.addHeader(AuthService.AUTHORIZATION_HEADER, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        User user = new User();
        user.setId(42L);
        user.setUsername("john");
        user.setRole(new Role(1L, "Student"));
        when(authService.authenticateSession("Bearer valid-token")).thenReturn(Optional.of(user));

        sessionFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(user, authentication.getPrincipal());
    }

    @Test
    void loginEndpointIsExcludedFromSessionValidation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        sessionFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(authService, never()).authenticateSession(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void refreshEndpointIsExcludedFromSessionValidation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        sessionFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(authService, never()).authenticateSession(org.mockito.ArgumentMatchers.anyString());
    }
}

