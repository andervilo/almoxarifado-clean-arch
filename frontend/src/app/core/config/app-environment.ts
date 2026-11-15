export const APP_ENVIRONMENT = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  keycloak: {
    baseUrl: 'http://localhost:8181',
    realm: 'almoxarifado',
    clientId: 'almoxarifado-frontend',
    redirectUri: `${window.location.origin}/login`,
    postLogoutRedirectUri: `${window.location.origin}/login`,
    scope: 'openid profile email',
  },
} as const;
