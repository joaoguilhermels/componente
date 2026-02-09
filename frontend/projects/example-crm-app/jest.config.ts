import type { Config } from 'jest';

const config: Config = {
  displayName: 'example-crm-app',
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
  moduleNameMapper: {
    'customer-registry-ui': '<rootDir>/../customer-registry-ui/src/public-api.ts',
  },
};

export default config;
