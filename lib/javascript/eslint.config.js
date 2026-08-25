import { defineConfig, globalIgnores } from "eslint/config";
import globals from "globals";
import js from "@eslint/js";
import tseslint from "typescript-eslint";
import eslintConfigPrettier from "eslint-config-prettier/flat";

export default defineConfig([
	globalIgnores(["dist/", "node_modules/"]),
	{
		files: ["**/*.{js,ts}"],
		languageOptions: { globals: globals.node },
		plugins: { js },
		extends: ["js/recommended"],
	},
	tseslint.configs.recommended,
	eslintConfigPrettier,
]);
