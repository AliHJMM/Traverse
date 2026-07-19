interface RuntimeEnv {
  stripePublishableKey: string;
}

declare global {
  interface Window {
    __env__?: RuntimeEnv;
  }
}

/**
 * Publishable keys are meant to be public (Stripe's own docs say so) -- this
 * is injected at container startup via nginx (envsubst on env.js.template),
 * not baked into the build, so the same image works across environments
 * without a rebuild.
 */
export function getStripePublishableKey(): string {
  return window.__env__?.stripePublishableKey ?? 'pk_test_placeholder';
}
