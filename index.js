import React from 'react';
import PropTypes from 'prop-types';
import {
  Animated,
  Platform,
  Text,
  TextInput,
  findNodeHandle,
  NativeModules,
} from 'react-native';

const { RNCustomFonts } = NativeModules;

const CustomFontsContext = React.createContext(
  null,
);

const sanitizeFontFaces = (fontFaces = []) => fontFaces.reduce(
  (arr, { uri, fontFamily, fontWeight }) => {
    // TODO: throw on unspecified here!
    const w = fontWeight || 'Normal';
    return [
      ...arr,
      {
        uri,
        fontFamily,
        fontWeight: (Platform.OS === 'ios') ? fontWeight : fontWeight.toLowerCase(),
      },
    ];
  },
  [],
);

class CustomFontsProvider extends React.Component {
  constructor(props) {
    super(props);
    this.__onRequestFontFamily = this.__onRequestFontFamily.bind(this);
  }
  async componentDidMount() {
    const {
      fontFaces,
      onDownloadDidStart,
      onDownloadDidEnd,
      onDownloadDidError,
    } = this.props;
    if (fontFaces && fontFaces.length > 0) {
      onDownloadDidStart();
      return RNCustomFonts
        .onFontFacesChanged(
          sanitizeFontFaces(
            fontFaces,
          ),
        )
        .then(onDownloadDidEnd)
        .catch(onDownloadDidError);
    }
    return Promise.resolve();
  }
  async componentWillUpdate(nextProps, nextState) {
    const {
      fontFaces,
      onDownloadDidStart,
      onDownloadDidEnd,
      onDownloadDidError,
    } = nextProps;
    const fontFacesDidChange = fontFaces !== this.props.fontFaces;
    if (fontFacesDidChange) {
      onDownloadDidStart();
      return RNCustomFonts
        .onFontFacesChanged(
          sanitizeFontFaces(
            fontFaces,
          ),
        )
        .then(onDownloadDidEnd)
        .catch(onDownloadDidError);
    }
    return Promise.resolve();
  }
  __isFontFamilyKnown(fontFamily) {
    const { fontFaces } = this.props;
    return (fontFaces || [])
      .reduce(
        (known, fontFace) => {
          if (typeof fontFace === 'object') {
            const {
              fontFamily: font,
              uri,
            } = fontFace;
            return (known || (fontFamily === font));
          }
          return known;
        },
        false,
      );
  }
  __onRequestFontFamily(ref, fontFamily, fontWeight) {
    const { latency } = this.props;
    return Promise.resolve()
      .then(() => {
        if (this.__isFontFamilyKnown(fontFamily)) {
          // XXX: Android does not obey the handle until after a render timeout.
          // TODO: How to enforce determinism?
          return new Promise(resolve => setTimeout(resolve, latency))
            .then(() => {
              return RNCustomFonts
                .onRequestFontFamily(
                  findNodeHandle(
                    ref,
                  ),
                  fontFamily,
                  fontWeight,
                );
            })
            .then(() => this.setState({}));
        }
        return Promise.reject(
          new Error(
            `Have requested fontFamily "${fontFamily}", but it does not exist. You should add the declaration to the CustomFontProvider's fontFaces prop.`,
          ),
        );
      });
  }
  render() {
    const {
      children,
      fontFaces,
      fadeDuration,
      ...extraProps
    } = this.props;
    return (
      <CustomFontsContext.Provider
        value={{
          requestFontFamily: this.__onRequestFontFamily,
          fadeDuration,
        }}
      >
        {children}
      </CustomFontsContext.Provider>
    );
  }
}

CustomFontsProvider.propTypes = {
  fontFaces: PropTypes.arrayOf(
    PropTypes.shape({}),
  ),
  latency: PropTypes.number,
  fadeDuration: PropTypes.number,
  onDownloadDidStart: PropTypes.func,
  onDownloadDidEnd: PropTypes.func,
};

CustomFontsProvider.defaultProps = {
  fontFaces: [],
  fadeDuration: 250,
  latency: 50,
  onDownloadDidStart: () => null,
  onDownloadDidEnd: () => null,
  onDownloadDidError: () => null,
};

const withCustomFont = FontConsumer => {
  class CustomFontConsumer extends React.Component {
    static contextType = CustomFontsContext;
    constructor(props) {
      super(props);
      this.__onRef = this.__onRef.bind(this);
      this.__onLayout = this.__onLayout.bind(this);
      this.state = {
        child: null,
        animOpacity: new Animated.Value(1),
        fontStyle: {},
      };
    }
    async componentWillUpdate(nextProps, nextState) {
      const {
        style,
      } = nextProps;
      const resolvedStyle = style || {};
      const {
        fontFamily,
        // TODO: Implement support.
        fontWeight,
      } = resolvedStyle;
      const {
        requestFontFamily,
        fadeDuration,
      } = this.context;
      const { child } = nextState;
      const didReceiveChild = child && !this.state.child;
      const didChangeFontFamily = fontFamily !== (this.props.style || {}).fontFamily;
      const didChangeFontWeight = fontWeight !== (this.props.style || {}).fontWeight;
      if ((fontFamily && child) && (didReceiveChild || didChangeFontFamily || didChangeFontWeight)) {
        return Promise.resolve()
          .then(() => requestFontFamily(
            child,
            fontFamily,
            fontWeight,
          ))
          .then(() => this.__shouldAnimateOpacity(Platform.OS === 'ios' ? 0 : 1, fadeDuration))
          .then(() => new Promise(resolve => this.setState({
            fontStyle: {
              fontWeight: Platform.OS === 'ios' ? fontWeight : undefined,
              fontFamily: Platform.OS === 'ios' ? fontFamily : undefined,
            },
          }, resolve)))
          .then(() => this.__shouldAnimateOpacity(1, fadeDuration))
          // TODO: Implement a catch here.
      }
      return Promise.resolve();
    }
    __shouldAnimateOpacity(toValue, duration) {
      const { animOpacity } = this.state;
      return new Promise(resolve => Animated.timing(
        animOpacity,
        {
          toValue,
          duration,
          useNativeDriver: true,
        },
      ).start(resolve));
    }
    __onRef(child) {
      this.setState(
        {
          child,
        },
      );
    }
    __onLayout(e) {

    }
    render() {
      const {
        style,
        ...extraProps
      } = this.props;
      const resolvedStyle = style || {};
      const {
        fontFamily,
        fontWeight,
        ...extraStyles
      } = resolvedStyle;
      const {
        animOpacity,
        fontStyle,
      } = this.state;
      return (
        <Animated.View
          style={{
            opacity: animOpacity,
          }}
        >
          <FontConsumer
            style={{
              ...extraStyles,
              ...fontStyle,
            }}
            onLayout={this.__onLayout}
            ref={this.__onRef}
            {...extraProps}
          />
        </Animated.View>
      );
    }
  };
  CustomFontConsumer.propTypes = {
    style: PropTypes.shape({}),
  };
  CustomFontConsumer.defaultProps = {
    style: {},
  };
  return CustomFontConsumer;
};

module.exports = {
  CustomFontsProvider,
  withCustomFont,
  TextInput: withCustomFont(TextInput),
  Text: withCustomFont(Text),
};
