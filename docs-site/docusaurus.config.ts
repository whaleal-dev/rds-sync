import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

/** 公开文档站：https://docs.whaleal.com/rds-sync/ */
const siteUrl = process.env.DOCS_SITE_URL ?? 'https://docs.whaleal.com';
const siteBaseUrl = process.env.DOCS_SITE_BASE_URL ?? '/rds-sync/';

const config: Config = {
  title: 'rds-sync 文档站',
  tagline: '关系型数据库同步 SDK · MySQL / Oracle / PostgreSQL → MySQL 或 Kafka',
  favicon: 'img/favicon.png',
  future: {v4: true},
  url: siteUrl,
  baseUrl: siteBaseUrl,
  organizationName: 'whaleal-dev',
  projectName: 'rds-sync',
  onBrokenLinks: 'throw',

  // 手册是纯 GFM Markdown：正文里有 <module>、<version> 这类占位符和原生 HTML 横幅。
  // 按 MDX 解析会把它们当成 JSX 表达式而构建失败，所以显式声明用 CommonMark。
  markdown: {format: 'md'},

  i18n: {
    defaultLocale: 'zh-Hans',
    locales: ['zh-Hans'],
  },
  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: 'docs',
          sidebarPath: './sidebars.ts',
          // 站点内容由 scripts/sync-docs.mjs 从仓库 docs/ 生成，编辑入口指回真源
          editUrl: 'https://github.com/whaleal-dev/rds-sync/tree/main/docs/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],
  themes: [
    [
      require.resolve('@easyops-cn/docusaurus-search-local'),
      {
        hashed: true,
        language: ['zh', 'en'],
        indexDocs: true,
        indexBlog: false,
        docsRouteBasePath: 'docs',
        searchResultLimits: 10,
        searchResultContextMaxLength: 80,
      },
    ],
  ],
  themeConfig: {
    navbar: {
      title: 'rds-sync',
      logo: {
        alt: 'Whaleal',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: '使用手册',
        },
        {
          type: 'search',
          position: 'right',
        },
        {
          href: 'https://github.com/whaleal-dev/rds-sync',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '手册',
          items: [
            {label: '手册首页', to: '/docs/'},
            {label: '当前落地状态', to: '/docs/reference/roadmap'},
            {label: '配置项全解', to: '/docs/guide/configuration'},
            {label: 'SDK 与 SPI', to: '/docs/guide/api'},
          ],
        },
        {
          title: '资源',
          items: [
            {label: 'GitHub', href: 'https://github.com/whaleal-dev/rds-sync'},
            {label: 'Issues', href: 'https://github.com/whaleal-dev/rds-sync/issues'},
            {label: '仓库 README', href: 'https://github.com/whaleal-dev/rds-sync#readme'},
            {label: '姊妹产品 mongo-sync', href: 'https://docs.whaleal.com/mongo-sync/'},
          ],
        },
      ],
      copyright: `Copyright (c) ${new Date().getFullYear()} whaleal-dev · 基于 Docusaurus 构建`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'bash', 'json', 'properties'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
