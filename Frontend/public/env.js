// Runtime config, not baked into the build. Overwritten by nginx's entrypoint
// (envsubst on env.js.template, driven by STRIPE_PUBLISHABLE_KEY) when this
// app runs in Docker. This placeholder default is only for local `ng serve`.
window.__env__ = {
  stripePublishableKey: 'pk_test_placeholder',
};
