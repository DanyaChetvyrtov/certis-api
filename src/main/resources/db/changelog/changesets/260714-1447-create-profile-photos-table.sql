--liquibase formatted sql

--changeset dasemenov:260714-1447-create-profile-photos-table
CREATE TABLE keeper.profile_photos
(
    id                 UUID PRIMARY KEY,
    profile_id         UUID         NOT NULL UNIQUE REFERENCES keeper.profiles (id),
    original_file_name VARCHAR(255) NOT NULL,
    extension          VARCHAR(10)  NOT NULL,
    file_size          BIGINT       NOT NULL,
    width              INTEGER      NOT NULL,
    height             INTEGER      NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    url                TEXT         NOT NULL,
    uploaded_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_profile_photos_original_file_name_not_blank
        CHECK (btrim(original_file_name) <> ''),
    CONSTRAINT chk_profile_photos_extension
        CHECK (extension IN ('jpg', 'jpeg', 'png', 'webp')),
    CONSTRAINT chk_profile_photos_file_size_positive
        CHECK (file_size > 0),
    CONSTRAINT chk_profile_photos_width_positive
        CHECK (width > 0),
    CONSTRAINT chk_profile_photos_height_positive
        CHECK (height > 0),
    CONSTRAINT chk_profile_photos_content_type
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT chk_profile_photos_url_not_blank
        CHECK (btrim(url) <> '')
);

--rollback DROP TABLE keeper.profile_photos;
