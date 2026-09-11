// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

/** Event types accepted by webhook endpoint subscriptions. */
public final class WebhookEventType {
  /** A session was created. */
  public static final String SESSION_CREATED = "session.created";

  /** A session was updated. */
  public static final String SESSION_UPDATED = "session.updated";

  /** A session was archived. */
  public static final String SESSION_ARCHIVED = "session.archived";

  /** A session was deleted. */
  public static final String SESSION_DELETED = "session.deleted";

  /** A session run started. */
  public static final String SESSION_STATUS_RUN_STARTED = "session.status_run_started";

  /** A session entered the idle state. */
  public static final String SESSION_STATUS_IDLED = "session.status_idled";

  /** A session entered the terminated state. */
  public static final String SESSION_STATUS_TERMINATED = "session.status_terminated";

  /** A session thread was created. */
  public static final String SESSION_THREAD_CREATED = "session.thread_created";

  /** A session thread run started. */
  public static final String SESSION_THREAD_RUN_STARTED = "session.thread_run_started";

  /** A session thread entered the idle state. */
  public static final String SESSION_THREAD_IDLED = "session.thread_idled";

  /** A session thread entered the terminated state. */
  public static final String SESSION_THREAD_TERMINATED = "session.thread_terminated";

  /** An agent was created. */
  public static final String AGENT_CREATED = "agent.created";

  /** An agent was updated. */
  public static final String AGENT_UPDATED = "agent.updated";

  /** An agent was archived. */
  public static final String AGENT_ARCHIVED = "agent.archived";

  /** A deployment was created. */
  public static final String DEPLOYMENT_CREATED = "deployment.created";

  /** A deployment was updated. */
  public static final String DEPLOYMENT_UPDATED = "deployment.updated";

  /** A deployment was archived. */
  public static final String DEPLOYMENT_ARCHIVED = "deployment.archived";

  /** A deployment was paused. */
  public static final String DEPLOYMENT_PAUSED = "deployment.paused";

  /** A deployment was resumed. */
  public static final String DEPLOYMENT_UNPAUSED = "deployment.unpaused";

  /** A deployment run started. */
  public static final String DEPLOYMENT_RUN_STARTED = "deployment_run.started";

  /** A deployment run failed. */
  public static final String DEPLOYMENT_RUN_FAILED = "deployment_run.failed";

  /** A deployment run succeeded. */
  public static final String DEPLOYMENT_RUN_SUCCEEDED = "deployment_run.succeeded";

  /** An environment was created. */
  public static final String ENVIRONMENT_CREATED = "environment.created";

  /** An environment was updated. */
  public static final String ENVIRONMENT_UPDATED = "environment.updated";

  /** An environment was archived. */
  public static final String ENVIRONMENT_ARCHIVED = "environment.archived";

  /** An environment was deleted. */
  public static final String ENVIRONMENT_DELETED = "environment.deleted";

  /** A vault was created. */
  public static final String VAULT_CREATED = "vault.created";

  /** A vault was archived. */
  public static final String VAULT_ARCHIVED = "vault.archived";

  /** A vault was deleted. */
  public static final String VAULT_DELETED = "vault.deleted";

  /** A vault credential was created. */
  public static final String VAULT_CREDENTIAL_CREATED = "vault_credential.created";

  /** A vault credential was archived. */
  public static final String VAULT_CREDENTIAL_ARCHIVED = "vault_credential.archived";

  /** A vault credential was deleted. */
  public static final String VAULT_CREDENTIAL_DELETED = "vault_credential.deleted";

  private WebhookEventType() {}
}
