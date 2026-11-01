# Cloudinary credential rotation

The Cloudinary API key and API secret must be supplied through the runtime environment:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
```

Do not place these values in `application.properties`, frontend `.env` files, API responses, logs, or committed CI configuration.

## Rotation procedure

1. In the Cloudinary console, revoke or rotate the exposed API secret.
2. Update the backend secret-manager or deployment environment with the new secret.
3. Keep the frontend environment limited to HOSTELOS API configuration; it does not need Cloudinary credentials.
4. Restart the backend so configuration binding fails fast or loads the new values.
5. Verify only through a controlled non-production upload using the rotated secret.
6. Review repository history, CI logs, build artifacts, and deployment logs for accidental exposure.

This repository must not rewrite Git history or force-push to remove historical exposure. Coordinate any historical secret-removal process through the repository security owner.
