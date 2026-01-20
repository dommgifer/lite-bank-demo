import { useTranslation } from 'react-i18next'
import { GlobeAltIcon } from '@heroicons/react/24/outline'

const languages = [
  { code: 'en', label: 'EN', fullName: 'English' },
  { code: 'zh-TW', label: '中', fullName: '繁體中文' }
]

export default function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const currentLang = languages.find(lang => lang.code === i18n.language) || languages[0]

  const toggleLanguage = () => {
    const currentIndex = languages.findIndex(lang => lang.code === i18n.language)
    const nextIndex = (currentIndex + 1) % languages.length
    i18n.changeLanguage(languages[nextIndex].code)
  }

  return (
    <button
      onClick={toggleLanguage}
      className="flex items-center space-x-1 px-3 py-2 rounded-xl hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
      title={`Switch to ${languages.find(l => l.code !== currentLang.code)?.fullName}`}
    >
      <GlobeAltIcon className="w-5 h-5 text-text/70" />
      <span className="text-sm font-medium text-text/70">{currentLang.label}</span>
    </button>
  )
}
