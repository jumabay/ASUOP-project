import {
  BarChartOutlined,
  DatabaseOutlined,
  EyeOutlined,
  ToolOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import { Layout, Menu, Tooltip, Typography } from 'antd'
import type { ReactNode } from 'react'
import type { ListTab } from '../utils/deepLink'

const { Sider, Header, Content } = Layout
const { Text, Title } = Typography

const STUB_TOOLTIP = 'Появится после утверждения модуля'

export interface AppShellProps {
  activeTab: ListTab
  onSelectFleetTab: (tab: ListTab) => void
  /** Safe backend data-source label (no JDBC URL / credentials). */
  dataSourceLabel?: string
  headerExtra?: ReactNode
  children: ReactNode
}

export function AppShell({
  activeTab,
  onSelectFleetTab,
  dataSourceLabel,
  headerExtra,
  children,
}: AppShellProps) {
  return (
    <Layout className="app-shell">
      <Sider
        className="app-sider"
        width={220}
        breakpoint="lg"
        collapsedWidth={64}
        theme="dark"
      >
        <div className="app-sider-brand" data-testid="app-sider-brand">
          <Text className="brand">АСУВП</Text>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[activeTab]}
          onClick={({ key }) => {
            if (key === 'watched' || key === 'registry') {
              onSelectFleetTab(key)
            }
          }}
          items={[
            {
              key: 'fleet',
              type: 'group',
              label: 'Парк',
              children: [
                {
                  key: 'watched',
                  icon: <EyeOutlined />,
                  label: 'Слежение',
                },
                {
                  key: 'registry',
                  icon: <UnorderedListOutlined />,
                  label: 'Реестр',
                },
              ],
            },
            {
              key: 'stubs',
              type: 'group',
              label: 'Модули',
              children: [
                {
                  key: 'repairs',
                  icon: <ToolOutlined />,
                  disabled: true,
                  label: (
                    <Tooltip title={STUB_TOOLTIP} placement="right">
                      <span data-testid="nav-stub-repairs">Ремонты</span>
                    </Tooltip>
                  ),
                },
                {
                  key: 'warehouse',
                  icon: <DatabaseOutlined />,
                  disabled: true,
                  label: (
                    <Tooltip title={STUB_TOOLTIP} placement="right">
                      <span data-testid="nav-stub-warehouse">Склад</span>
                    </Tooltip>
                  ),
                },
                {
                  key: 'reports',
                  icon: <BarChartOutlined />,
                  disabled: true,
                  label: (
                    <Tooltip title={STUB_TOOLTIP} placement="right">
                      <span data-testid="nav-stub-reports">Отчёты</span>
                    </Tooltip>
                  ),
                },
              ],
            },
          ]}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <div className="app-header-row">
            <div>
              <Title level={4} className="header-title">
                Реестр, слежение и карточка вагона
              </Title>
              <Text
                type="secondary"
                className="header-meta"
                data-testid="data-source-label"
              >
                {dataSourceLabel?.trim() ? dataSourceLabel : 'Источник…'}
              </Text>
            </div>
            {headerExtra}
          </div>
        </Header>
        <Content className="app-content">{children}</Content>
      </Layout>
    </Layout>
  )
}
