import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import styles from './index.module.css';

const quickRoutes = [
  {
    title: '当前落地状态',
    description: 'P0 架构契约阶段：哪些已定稿、哪些还没接到产品 SPI。先看这个再判断能不能用。',
    to: '/docs/reference/roadmap',
  },
  {
    title: '快速开始',
    description: '构建、契约单测，以及现在真正能跑的部分。',
    to: '/docs/getting-started/quickstart',
  },
  {
    title: '事件契约与 SPI',
    description: 'RowChange / DdlEvent、RowChangeSink 与 Kafka 消息契约。',
    to: '/docs/guide/api',
  },
  {
    title: '配置项全解',
    description: '全部配置键与 Builder 方法、默认值、当前落地状态。',
    to: '/docs/guide/configuration',
  },
];

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  const bannerUrl = useBaseUrl('/img/banner.svg');
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <img className={styles.banner} src={bannerUrl} alt="rds-sync" />
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link className="button button--secondary button--lg" to="/docs/">
            开始阅读手册
          </Link>
          <Link className="button button--info button--lg" to="/docs/reference/roadmap">
            当前落地状态
          </Link>
        </div>
      </div>
    </header>
  );
}

function QuickNavigation() {
  return (
    <section className={styles.quickRoutes} aria-labelledby="quick-routes-heading">
      <div className="container">
        <Heading as="h2" id="quick-routes-heading">
          按任务进入
        </Heading>
        <p className={styles.quickRoutesIntro}>
          关系型数据库同步 SDK：MySQL / Oracle / PostgreSQL 全量源，Sink 为 MySQL JDBC 或 Kafka。
          控制面与 mongo-sync 同构，数据面互不替代。
        </p>
        <div className={styles.quickRouteGrid}>
          {quickRoutes.map((route) => (
            <Link className={styles.quickRoute} key={route.to} to={route.to}>
              <Heading as="h3">{route.title}</Heading>
              <p>{route.description}</p>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={siteConfig.title}
      description="rds-sync 使用手册：关系型数据库同步 SDK 的配置、同步模式、事件契约与 SPI、落地状态与排障。">
      <HomepageHeader />
      <main>
        <QuickNavigation />
      </main>
    </Layout>
  );
}
