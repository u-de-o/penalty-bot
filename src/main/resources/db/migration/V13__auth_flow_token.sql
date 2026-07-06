-- Repurpose the single-guild config session token into a generic auth-flow token.
-- It now bridges the OAuth endpoints and the Vaadin session (CSRF state + user handoff);
-- the target guild is no longer baked into the token but chosen interactively.

alter table config_session_token rename to auth_flow_token;
alter sequence config_session_token_seq rename to auth_flow_token_seq;

alter table auth_flow_token rename constraint pk_config_session_token to pk_auth_flow_token;
alter table auth_flow_token rename constraint uq_config_session_token_token to uq_auth_flow_token_token;

alter table auth_flow_token drop column guild_id;
alter table auth_flow_token alter column user_id drop not null;

-- Stale rows from the previous flow are meaningless under the new semantics.
delete from auth_flow_token;
