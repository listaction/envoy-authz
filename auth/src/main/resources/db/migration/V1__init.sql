create table acls (
    id varchar(255) not null
        constraint acls_pkey
            primary key,
    created bigint,
    namespace varchar(255),
    nsobject varchar(255),
    object varchar(255),
    relation varchar(255),
    updated bigint,
    usr varchar(255),
    userset_namespace varchar(255),
    userset_object varchar(255),
    userset_relation varchar(255)
);

create table configs (
    id varchar(255) not null
        constraint configs_pkey
            primary key,
    config jsonb,
    namespace varchar(255)
);

create table mappings (
    id varchar(255) not null
        constraint mapping_pkey
            primary key,
    body_mapping jsonb,
    header_mapping jsonb,
    method varchar(255),
    namespace varchar(255),
    object varchar(255),
    path varchar(255),
    roles jsonb
);

create table rel_cache (
    id varchar(250)
         constraint rel_cache_pkey
             primary key,
    nsobject varchar(250),
    relation varchar(50),
    nested_relations jsonb,
    path varchar(250),
    usr varchar(36),
    rev bigint
);

create table resources (
    id varchar(255) not null
        constraint resources_pkey
            primary key,
    namespace varchar(255),
    resource_id varchar(255),
    created bigint,
    updated bigint
);

create table roles (
    id varchar(255) not null
         constraint roles_pkey
             primary key,
    permissions jsonb,
    created bigint,
    updated bigint
);

create index acls_namespace_object_usr_index
    on acls (namespace, object, usr);

create index acls_nsobject_usr_index
    on acls (nsobject, usr);

create index acls_usr_index
    on acls (usr);

create index acls_nsobject_index
    on acls (nsobject);

create index rel_cache_usr_rev_index
    on rel_cache (usr, rev);

create index rel_cache_usr_rev_nsobject_path_index
    on rel_cache (usr, rev, nsobject, path);

create index resources_namespace_resourceid_index
    on resources (namespace, resource_id);
