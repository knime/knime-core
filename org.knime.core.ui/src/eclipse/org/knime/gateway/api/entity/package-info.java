/**
 * This package contains entities which are used as messages between the core (i.e. backend) and the UI (i.e. frontend).
 * The layer in between is usually called 'gateway API' and defined somewhere else (not accessible from here).
 *
 * The majority of entities is only required by the gateway API itself. However, there is a very small subset of
 * entities which are also used somewhere else (usually backport some functionality into the 'classic' AP, such as node
 * views).
 */
package org.knime.gateway.api.entity;