#!/bin/sh
set -e

envsubst '${STRIPE_PUBLISHABLE_KEY}' < /usr/share/nginx/html/env.js.template > /usr/share/nginx/html/env.js

exec nginx -g 'daemon off;'
