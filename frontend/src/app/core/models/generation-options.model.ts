export interface GenerationOptions {
  sql: string;
  packageName: string;
  generateJwt: boolean;
  generatePagination: boolean;
  generateSoftDelete: boolean;
  enrichWithLlm: boolean;
}
